"""End-to-end: catalog -> market sync -> orders and stock counts -> sync -> market listing."""
import unittest

from tests.support import VendorHome, app_with_catalog, load_catalog, order


class MarketSyncTest(unittest.TestCase):
    def setUp(self):
        self.home = VendorHome()
        self.app = app_with_catalog(self.home, load_catalog())
        first = self.app.post("/market/sync", query={"full": "1"})
        self.assertEqual(first.status, 200, first.body)
        # a day of trading
        order(self.app, "GD-104", 2, ship=True)       # 3 -> 1 left, which is the CS unit
        order(self.app, "VC-2003", 3)                   # 4 on hand, 3 reserved
        self.app.post("/products/FD-303/count", {"counted": 0})   # shelf was empty
        order(self.app, "GD-102", 1, ship=True)        # 5 -> 4
        self.app.post("/products/GD-105/receive", {"qty": 10})    # 7 -> 17
        cancelled = order(self.app, "FD-302", 2)
        self.app.post(f"/orders/{cancelled}/cancel")
        self.sync = self.app.post("/market/sync")

    def tearDown(self):
        self.home.cleanup()

    def items(self):
        resp = self.app.get("/market/listing")
        self.assertEqual(resp.status, 200, resp.body)
        return {i["sku"]: i for i in resp.body["items"]}

    def test_sync_pushes_every_changed_product(self):
        self.assertEqual(self.sync.status, 200, self.sync.body)
        self.assertEqual(
            sorted(self.sync.body["skus"]),
            ["FD-303", "GD-102", "GD-104", "GD-105", "VC-2003"],
        )

    def test_sold_out_products_are_not_on_sale(self):
        items = self.items()
        for sku in ("GD-104", "VC-2003", "FD-303"):
            self.assertFalse(items[sku]["on_sale"], f"{sku} is sold out but still on sale in the market")
            self.assertEqual(items[sku]["market_qty"], 0, sku)

    def test_products_in_stock_show_their_quantity(self):
        items = self.items()
        expected = {"GD-101": 11, "GD-102": 3, "GD-105": 16, "VC-2001": 39, "FD-302": 5, "DG-403": 7}
        for sku, qty in expected.items():
            self.assertTrue(items[sku]["on_sale"], sku)
            self.assertEqual(items[sku]["market_qty"], qty, sku)

    def test_market_matches_our_stock(self):
        drift = self.app.get("/reports/market-drift").body
        self.assertEqual(drift["items"], [], "market quantities differ from the stock we offer")

    def test_product_page_shows_market_state(self):
        body = self.app.get("/products/gd-104").body
        self.assertEqual(body["on_hand"], 1)
        self.assertFalse(body["market"]["on_sale"], "sold-out product page still says on sale")

    def test_quiet_sync_sends_nothing(self):
        again = self.app.post("/market/sync").body
        self.assertEqual(again["lines"], 0)
        self.assertIsNone(again["transfer"])

    def test_low_stock_alerts(self):
        alerts = {a["sku"]: a["level"] for a in self.app.get("/reports/low-stock").body["items"]}
        self.assertEqual(alerts["FD-303"], "out")
        self.assertEqual(alerts["GD-104"], "low")
        self.assertNotIn("GD-101", alerts)


if __name__ == "__main__":
    unittest.main()
