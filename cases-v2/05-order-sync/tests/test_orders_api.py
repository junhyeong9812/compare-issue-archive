import unittest

from tests.support import Shop


class OrdersApiTest(unittest.TestCase):
    def setUp(self):
        self.shop = Shop(orders=3)
        self.app = self.shop.app

    def test_get_and_cancel(self):
        oid = self.shop.newest_first()[0]
        self.assertEqual(self.app.get(f"/orders/{oid}").status, 200)
        self.assertEqual(self.app.post(f"/orders/{oid}/cancel").status, 200)
        self.assertEqual(self.app.post(f"/orders/{oid}/cancel").status, 400)
        self.assertEqual(self.app.post("/orders/999/cancel").status, 404)

    def test_validation(self):
        self.assertEqual(self.app.post("/orders", {"customer": "x", "amount": 0}).status, 400)
        self.assertEqual(self.app.post("/orders", {"customer": "x", "amount": 5, "channel": "fax"}).status, 400)
        future = self.shop.clock.now() + 3600
        self.assertEqual(self.app.post("/orders", {"customer": "x", "amount": 5, "created_at": future}).status, 400)

    def test_listing_limits(self):
        self.assertEqual(self.app.get("/orders", {"limit": 0}).status, 400)
        self.assertEqual(self.app.get("/orders", {"limit": 5000}).status, 400)
        body = self.app.get("/orders", {"limit": 2}).body
        self.assertEqual(len(body["items"]), 2)
        last = self.app.get("/orders", {"limit": 2, "cursor": body["next_cursor"]}).body
        self.assertEqual(len(last["items"]), 1)
        self.assertIsNone(last["next_cursor"])

    def test_garbage_cursor_is_400(self):
        self.assertEqual(self.app.get("/orders", {"cursor": "%%%"}).status, 400)
        self.assertEqual(self.app.get("/orders", {"cursor": "e30"}).status, 400)

    def test_daily_report(self):
        body = self.app.get("/reports/daily").body
        self.assertEqual(sum(d["orders"] for d in body["days"]), 3)
        self.assertEqual(body["days"][0]["display"], "3,003 KRW")
