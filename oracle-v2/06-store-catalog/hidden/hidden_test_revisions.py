"""Hidden acceptance: a rolled-back revision restores exactly that store's catalog as it was."""
import unittest

from tests.support import listing, make_app


def tick(app, times=1):
    for _ in range(times):
        app.post("/jobs/tick")


def drain(app):
    for _ in range(50):
        if not app.post("/jobs/tick").body["ran"]:
            return
    raise AssertionError("jobs did not finish")


class HiddenRevisionTest(unittest.TestCase):
    def setUp(self):
        self.app = make_app(self, import_batch_size=2)

    def test_deleted_product_stays_deleted_after_rejection(self):
        ok = self.app.post("/stores/seoul/changes", [{"op": "delete", "sku": "SNK-11"}])
        self.assertEqual(ok.status, 200, ok.body)
        before = listing(self.app, "seoul")
        self.assertNotIn("SNK-11", before)
        bad = self.app.post("/stores/seoul/changes", [{"op": "set_price", "sku": "TEA-01", "price": -1}])
        self.assertEqual(bad.status, 422)
        self.assertEqual(listing(self.app, "seoul"), before)

    def test_concurrent_imports_failed_one_restores_its_own_catalog(self):
        seoul_before = listing(self.app, "seoul")
        seoul_job = self.app.post("/stores/seoul/imports", [
            {"op": "set_price", "sku": "TEA-01", "price": 3500},
            {"op": "put", "sku": "CAK-01", "name": "Castella", "price": 6000, "stock": 4},
            {"op": "set_stock", "sku": "COF-02", "stock": -1},
        ]).body["job"]
        busan_job = self.app.post("/stores/busan/imports", [
            {"op": "set_price", "sku": "FSH-03", "price": 12500},
            {"op": "put", "sku": "TEA-02", "name": "Corn Tea", "price": 2900, "stock": 15},
        ]).body["job"]
        drain(self.app)
        self.assertEqual(self.app.get(f"/jobs/{seoul_job}").body["state"], "failed")
        self.assertEqual(self.app.get(f"/jobs/{busan_job}").body["state"], "done")
        self.assertEqual(listing(self.app, "seoul"), seoul_before)
        busan = listing(self.app, "busan")
        self.assertEqual(busan["FSH-03"][1], 12500)
        self.assertIn("TEA-02", busan)

    def test_import_rollback_after_other_branch_edits_in_between(self):
        busan_before = listing(self.app, "busan")
        job = self.app.post("/stores/busan/imports", [
            {"op": "delete", "sku": "SNK-20"},
            {"op": "put", "sku": "FSH-09", "name": "Dried Squid", "price": 0, "stock": 2},
        ]).body["job"]
        tick(self.app)  # job opened
        self.assertEqual(self.app.post("/stores/seoul/changes", [
            {"op": "set_price", "sku": "TEA-01", "price": 3200},
        ]).status, 200)
        self.assertEqual(self.app.post("/stores/seoul/changes", [
            {"op": "set_price", "sku": "BRD-07", "price": -5},
        ]).status, 422)
        drain(self.app)
        self.assertEqual(self.app.get(f"/jobs/{job}").body["state"], "failed")
        self.assertEqual(listing(self.app, "busan"), busan_before)
        seoul = listing(self.app, "seoul")
        self.assertEqual(seoul["TEA-01"][1], 3200)
        self.assertEqual(sorted(seoul), ["BRD-07", "COF-02", "GFT-10", "SNK-11", "TEA-01"])

    def test_same_branch_import_and_rejected_change(self):
        before = listing(self.app, "seoul")
        job = self.app.post("/stores/seoul/imports", [
            {"op": "set_price", "sku": "COF-02", "price": 4700},
            {"op": "delete", "sku": "BRD-07"},
            {"op": "put", "sku": "TEA-09", "name": "Barley Tea", "price": 3000},
        ]).body["job"]
        tick(self.app, 2)  # opened, first two edits applied
        self.assertEqual(self.app.post("/stores/seoul/changes", [
            {"op": "set_price", "sku": "SNK-11", "price": 0},
        ]).status, 422)
        drain(self.app)
        self.assertEqual(self.app.get(f"/jobs/{job}").body["state"], "failed")
        self.assertEqual(listing(self.app, "seoul"), before)


if __name__ == "__main__":
    unittest.main()
