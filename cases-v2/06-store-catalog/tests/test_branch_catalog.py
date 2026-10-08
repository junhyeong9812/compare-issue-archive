"""Branch catalog behaviour through the public API."""
import unittest

from tests.support import listing, make_app


class BranchCatalogTest(unittest.TestCase):
    def setUp(self):
        self.app = make_app(self)

    def test_listing_includes_shared_items(self):
        seoul = listing(self.app, "seoul")
        self.assertEqual(sorted(seoul), ["BRD-07", "COF-02", "GFT-10", "SNK-11", "TEA-01"])
        self.assertEqual(self.app.get("/stores").body["stores"], ["busan", "seoul"])

    def test_unknown_store_and_product(self):
        self.assertEqual(self.app.get("/stores/daegu/products").status, 404)
        self.assertEqual(self.app.get("/stores/seoul/products/NOPE-1").status, 404)
        self.assertEqual(self.app.get("/stores/_shared/products").status, 404)

    def test_price_update_is_published(self):
        resp = self.app.post("/stores/seoul/changes", [
            {"op": "set_price", "sku": "TEA-01", "price": 3300},
            {"op": "put", "sku": "TEA-05", "name": "Green Tea", "price": 3500, "stock": 10},
        ])
        self.assertEqual(resp.status, 200, resp.body)
        seoul = listing(self.app, "seoul")
        self.assertEqual(seoul["TEA-01"][1], 3300)
        self.assertIn("TEA-05", seoul)

    def test_invalid_update_is_rejected_whole(self):
        before = listing(self.app, "seoul")
        resp = self.app.post("/stores/seoul/changes", [
            {"op": "set_price", "sku": "TEA-01", "price": 3300},
            {"op": "set_stock", "sku": "BRD-07", "stock": -2},
        ])
        self.assertEqual(resp.status, 422)
        self.assertEqual(listing(self.app, "seoul"), before)

    def test_bad_edit_is_400_and_nothing_changes(self):
        before = listing(self.app, "busan")
        resp = self.app.post("/stores/busan/changes", [
            {"op": "delete", "sku": "SNK-20"},
            {"op": "set_price", "sku": "NOT-THERE", "price": 10},
        ])
        self.assertEqual(resp.status, 400)
        self.assertEqual(listing(self.app, "busan"), before)

    def test_rejected_update_after_other_branch_published(self):
        ok = self.app.post("/stores/busan/changes", [
            {"op": "put", "sku": "FSH-04", "name": "Grilled Eel", "price": 18000, "stock": 5},
        ])
        self.assertEqual(ok.status, 200, ok.body)

        before = listing(self.app, "seoul")
        resp = self.app.post("/stores/seoul/changes", [
            {"op": "set_price", "sku": "COF-02", "price": 4500},
            {"op": "set_price", "sku": "SNK-11", "price": 0},
        ])
        self.assertEqual(resp.status, 422)
        self.assertEqual(
            listing(self.app, "seoul"), before,
            "the seoul catalog is different after its update was rejected",
        )


if __name__ == "__main__":
    unittest.main()
