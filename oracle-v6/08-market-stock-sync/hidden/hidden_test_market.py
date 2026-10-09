"""Hidden judge: after every sync the market itself (read straight through the
vendor CLI, not only through our listing) must hold the quantity we offer for
every product — including products that run out, come back, or start empty."""
import json
import os
import shutil
import subprocess
import tempfile
import unittest

from hbstock.app import build_app

HERE = os.path.dirname(os.path.abspath(__file__))
STOCKPUSH = os.path.join(HERE, "vendor", "bin", "stockpush")

CATALOG = [
    {"sku": "GD-501", "name": "우산", "category": "goods", "price": 7000, "on_hand": 6},
    {"sku": "GD-502", "name": "담요", "category": "goods", "price": 9000, "on_hand": 2},
    {"sku": "GD-503", "name": "보조배터리", "category": "goods", "price": 15000, "on_hand": 1},
    {"sku": "GD-504", "name": "사전 예약 피규어", "category": "goods", "price": 30000, "on_hand": 0},
    {"sku": "VC-5101", "name": "베이커리 1만원권", "category": "voucher", "price": 10000, "on_hand": 5},
    {"sku": "VC-5102", "name": "주유 상품권", "category": "voucher", "price": 30000, "on_hand": 3},
    {"sku": "FD-551", "name": "한우 선물세트", "category": "food", "price": 89000, "on_hand": 4},
    {"sku": "FD-552", "name": "꿀 2병", "category": "food", "price": 21000, "on_hand": 11},
    {"sku": "DG-591", "name": "웹툰 쿠키 50개", "category": "digital", "price": 5000, "on_hand": 50},
]


def vendor_rows(home):
    env = dict(os.environ, STOCKPUSH_HOME=home)
    out = subprocess.run([STOCKPUSH, "list"], capture_output=True, text=True, env=env, check=True).stdout
    rows = {}
    for line in out.splitlines():
        sku, qty, st, _ = line.split("\t")
        rows[sku] = (int(qty), int(st))
    return rows


class MarketJudge(unittest.TestCase):
    def setUp(self):
        self.home = tempfile.mkdtemp(prefix="hidden-stockpush-")
        self.app = build_app(stockpush_home=self.home)

    def tearDown(self):
        shutil.rmtree(self.home, ignore_errors=True)

    def load(self, catalog):
        for payload in catalog:
            resp = self.app.post("/products", payload)
            self.assertEqual(resp.status, 201, resp.body)

    def sync(self, full=False):
        resp = self.app.post("/market/sync", query={"full": "1"} if full else {})
        self.assertEqual(resp.status, 200, resp.body)
        return resp.body

    def order(self, sku, qty, ship=False):
        resp = self.app.post("/orders", {"sku": sku, "qty": qty})
        self.assertEqual(resp.status, 201, resp.body)
        if ship:
            self.app.post(f"/orders/{resp.body['id']}/ship")
        return resp.body["id"]

    def want(self, product):
        return max(product.on_hand - product.reserved - self.app.settings.market_safety_stock, 0)

    def check_market(self):
        rows = vendor_rows(self.home)
        listing = {i["sku"]: i for i in self.app.get("/market/listing").body["items"]}
        for product in self.app.repo.products():
            want = self.want(product)
            row = rows.get(product.sku)
            if row is None:
                self.assertEqual(want, 0, f"{product.sku} is missing from the market")
            else:
                self.assertEqual(row[0], want, f"market quantity of {product.sku}")
                self.assertEqual(row[1], 1 if want > 0 else 2, f"market state of {product.sku}")
            self.assertEqual(listing[product.sku]["market_qty"], row[0] if row else 0)
            self.assertEqual(listing[product.sku]["on_sale"], want > 0, product.sku)
        self.assertEqual(self.app.get("/reports/market-drift").body["items"], [])

    def test_run_out_and_come_back(self):
        self.load(CATALOG)
        self.sync(full=True)
        self.check_market()
        self.order("GD-501", 5, ship=True)          # 6 -> 1: only the CS unit is left
        held = self.order("VC-5102", 2)             # reserved down to zero
        self.app.post("/products/FD-551/count", {"counted": 0})
        self.order("GD-502", 1, ship=True)          # 2 -> 1
        self.sync()
        self.check_market()
        self.app.post("/products/GD-501/receive", {"qty": 4})   # back in stock
        self.app.post(f"/orders/{held}/cancel")                 # reservation released
        self.app.post("/products/GD-504/receive", {"qty": 3})   # pre-order arrives
        self.sync()
        self.check_market()
        self.order("GD-504", 2, ship=True)                      # gone again
        self.sync()
        self.check_market()

    def test_full_resync_after_selling_out(self):
        self.load(CATALOG)
        self.sync(full=True)
        self.order("DG-591", 49, ship=True)
        self.order("VC-5101", 4)
        self.sync(full=True)
        self.check_market()

    def test_sample_catalog_day(self):
        with open(os.path.join(HERE, "tests", "fixtures", "catalog_2026_10.json"), encoding="utf-8") as f:
            catalog = json.load(f)
        self.load(catalog)
        self.sync(full=True)
        self.order("GD-104", 2, ship=True)
        self.order("VC-2003", 3)
        self.app.post("/products/FD-303/count", {"counted": 0})
        self.app.post("/products/GD-103/count", {"counted": 1})
        self.order("DG-402", 14)
        self.sync()
        self.check_market()
        rows = vendor_rows(self.home)
        self.assertEqual(rows["GD-101"], (11, 1))
        self.assertEqual(rows["DG-401"], (29, 1))


if __name__ == "__main__":
    unittest.main()
