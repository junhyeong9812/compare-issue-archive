"""Hidden judge for coming back on sale: a product that ran out goes back on sale
with the next incremental sync, but between two full syncs (one market day) the
market may see it come back (state 2 -> 1) at most once. Selling out is always
pushed at once. The market is read straight through the vendor CLI."""
import os
import shutil
import subprocess
import tempfile
import unittest

from hbstock.app import build_app

HERE = os.path.dirname(os.path.abspath(__file__))
STOCKPUSH = os.path.join(HERE, "vendor", "bin", "stockpush")

CATALOG = [
    {"sku": "GD-601", "name": "한정판 텀블러", "category": "goods", "price": 9900, "on_hand": 3},
    {"sku": "GD-602", "name": "캠핑 의자", "category": "goods", "price": 25000, "on_hand": 8},
    {"sku": "VC-611", "name": "영화 관람권", "category": "voucher", "price": 11000, "on_hand": 4},
    {"sku": "FD-621", "name": "견과류 선물세트", "category": "food", "price": 12900, "on_hand": 2},
    {"sku": "DG-631", "name": "음원 30일", "category": "digital", "price": 7900, "on_hand": 20},
]


def vendor_rows(home):
    env = dict(os.environ, STOCKPUSH_HOME=home)
    out = subprocess.run([STOCKPUSH, "list"], capture_output=True, text=True, env=env, check=True).stdout
    rows = {}
    for line in out.splitlines():
        sku, qty, st, _ = line.split("\t")
        rows[sku] = (int(qty), int(st))
    return rows


class RelistJudge(unittest.TestCase):
    def setUp(self):
        self.home = tempfile.mkdtemp(prefix="hidden-relist-")
        self.app = build_app(stockpush_home=self.home)
        for payload in CATALOG:
            resp = self.app.post("/products", payload)
            self.assertEqual(resp.status, 201, resp.body)
        self.state = {}
        self.comebacks = {}
        self.sync(full=True)

    def tearDown(self):
        shutil.rmtree(self.home, ignore_errors=True)

    def sync(self, full=False):
        resp = self.app.post("/market/sync", query={"full": "1"} if full else {})
        self.assertEqual(resp.status, 200, resp.body)
        rows = vendor_rows(self.home)
        if full:
            self.comebacks = {}
        else:
            for sku, (_, st) in rows.items():
                if self.state.get(sku) == 2 and st == 1:
                    self.comebacks[sku] = self.comebacks.get(sku, 0) + 1
                    self.assertLessEqual(self.comebacks[sku], 1, f"{sku} came back on sale twice in one market day")
        self.state = {sku: st for sku, (_, st) in rows.items()}
        listing = {i["sku"]: i for i in self.app.get("/market/listing").body["items"]}
        for sku, (qty, st) in rows.items():
            self.assertEqual(listing[sku]["market_qty"], qty, f"listing of {sku}")
            self.assertEqual(listing[sku]["on_sale"], st == 1, f"listing of {sku}")
        return rows

    def want(self, sku):
        p = self.app.repo.product(sku)
        return max(p.on_hand - p.reserved - self.app.settings.market_safety_stock, 0)

    def order(self, sku, qty, ship=False):
        resp = self.app.post("/orders", {"sku": sku, "qty": qty})
        self.assertEqual(resp.status, 201, resp.body)
        if ship:
            self.assertEqual(self.app.post(f"/orders/{resp.body['id']}/ship").status, 200)
        return resp.body["id"]

    def receive(self, sku, qty):
        self.assertEqual(self.app.post(f"/products/{sku}/receive", {"qty": qty}).status, 200)

    def assert_on_sale(self, rows, sku):
        self.assertEqual(rows[sku], (self.want(sku), 1), f"{sku} should be on sale with what we offer")

    def assert_off_sale(self, rows, sku):
        self.assertEqual(rows[sku], (0, 2), f"{sku} should be off sale")

    def test_comes_back_once_per_market_day(self):
        self.order("GD-601", 2, ship=True)             # 3 -> 1: only the CS unit
        self.assert_off_sale(self.sync(), "GD-601")

        self.receive("GD-601", 5)                       # back in stock
        rows = self.sync()
        self.assert_on_sale(rows, "GD-601")             # first comeback today: right away

        self.order("GD-601", 5, ship=True)              # gone again
        held = self.order("VC-611", 3)                  # 4 on hand, 3 reserved
        rows = self.sync()
        self.assert_off_sale(rows, "GD-601")            # selling out is immediate
        self.assert_off_sale(rows, "VC-611")

        self.receive("GD-601", 4)                       # second restock the same day
        self.app.post(f"/orders/{held}/cancel")         # VC-611 comes back for the first time
        rows = self.sync()
        self.assert_off_sale(rows, "GD-601")
        self.assert_on_sale(rows, "VC-611")

        self.receive("GD-601", 2)
        self.order("GD-602", 1, ship=True)              # unrelated change in the same sync
        rows = self.sync()
        self.assert_off_sale(rows, "GD-601")
        self.assert_on_sale(rows, "GD-602")

        rows = self.sync(full=True)                     # 06:00: new market day
        self.assert_on_sale(rows, "GD-601")
        for sku in ("GD-602", "VC-611", "FD-621", "DG-631"):
            self.assert_on_sale(rows, sku)

        self.order("FD-621", 1, ship=True)              # 2 -> 1: sold out in the new day
        self.assert_off_sale(self.sync(), "FD-621")
        self.receive("FD-621", 3)
        self.assert_on_sale(self.sync(), "FD-621")      # its first comeback of the new day
        self.assertEqual(self.app.get("/reports/market-drift").body["items"], [])

    def test_products_that_never_ran_out_keep_following_stock(self):
        for qty in (3, 4, 2):
            self.order("DG-631", qty, ship=True)
            self.assert_on_sale(self.sync(), "DG-631")
        self.receive("DG-631", 10)
        rows = self.sync()
        self.assert_on_sale(rows, "DG-631")
        self.assertEqual(rows["DG-631"], (20, 1))


if __name__ == "__main__":
    unittest.main()
