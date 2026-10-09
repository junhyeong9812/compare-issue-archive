import unittest

from hbstock.app import build_app


class InventoryAndOrdersTest(unittest.TestCase):
    def setUp(self):
        self.app = build_app(stockpush_home="/nonexistent")
        resp = self.app.post("/products", {"sku": " gd_201 ", "name": "볼펜", "price": "900", "on_hand": 4})
        self.assertEqual(resp.status, 201, resp.body)

    def product(self):
        return self.app.repo.product("GD-201")

    def test_create_normalizes_sku(self):
        self.assertEqual(self.product().sku, "GD-201")
        self.assertEqual(self.product().category, "goods")

    def test_create_validation(self):
        self.assertEqual(self.app.post("/products", {"sku": "X1", "name": "a", "price": 1}).status, 400)
        self.assertEqual(self.app.post("/products", {"sku": "GD-202", "name": "", "price": 1}).status, 400)
        self.assertEqual(self.app.post("/products", {"sku": "GD-202", "name": "a", "price": 0}).status, 400)
        self.assertEqual(self.app.post("/products", {"sku": "GD-201", "name": "a", "price": 1}).status, 409)

    def test_order_reserves_and_ship_takes_off_shelf(self):
        oid = self.app.post("/orders", {"sku": "GD-201", "qty": 3}).body["id"]
        self.assertEqual((self.product().on_hand, self.product().reserved), (4, 3))
        self.app.post(f"/orders/{oid}/ship")
        self.assertEqual((self.product().on_hand, self.product().reserved), (1, 0))
        self.assertEqual(self.app.post(f"/orders/{oid}/ship").status, 409)

    def test_cancel_releases(self):
        oid = self.app.post("/orders", {"sku": "GD-201", "qty": 2}).body["id"]
        self.app.post(f"/orders/{oid}/cancel")
        self.assertEqual(self.product().available, 4)

    def test_cannot_order_more_than_available(self):
        self.assertEqual(self.app.post("/orders", {"sku": "GD-201", "qty": 5}).status, 409)
        self.assertEqual(self.app.post("/orders", {"sku": "GD-201", "qty": "1.5"}).status, 400)

    def test_receive_and_count(self):
        self.app.post("/products/GD-201/receive", {"qty": 6})
        self.assertEqual(self.product().on_hand, 10)
        self.app.post("/products/GD-201/count", {"counted": 7})
        self.assertEqual(self.product().on_hand, 7)
        self.assertEqual(self.app.post("/products/GD-201/receive", {"qty": 0}).status, 400)
        kinds = [m["kind"] for m in self.app.repo.movements]
        self.assertEqual(kinds, ["create", "receive", "count"])


if __name__ == "__main__":
    unittest.main()
