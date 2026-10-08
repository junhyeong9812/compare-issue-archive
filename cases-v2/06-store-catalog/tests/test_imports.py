"""Background import jobs."""
import unittest

from tests.support import listing, make_app


def run_until_idle(app, limit=50):
    for _ in range(limit):
        if not app.post("/jobs/tick").body["ran"]:
            return
    raise AssertionError("jobs did not finish")


class ImportJobTest(unittest.TestCase):
    def setUp(self):
        self.app = make_app(self, import_batch_size=2)

    def test_large_import_publishes_everything(self):
        edits = [{"op": "put", "sku": f"SNK-{30 + i}", "name": f"Snack {i}", "price": 1000 + i, "stock": 3}
                 for i in range(7)]
        job = self.app.post("/stores/busan/imports", edits).body["job"]
        run_until_idle(self.app)
        self.assertEqual(self.app.get(f"/jobs/{job}").body["state"], "done")
        self.assertEqual(len(listing(self.app, "busan")), 3 + 7 + 1)

    def test_failed_import_leaves_catalog_as_it_was(self):
        before = listing(self.app, "busan")
        edits = [
            {"op": "put", "sku": "SNK-40", "name": "Snack A", "price": 900},
            {"op": "delete", "sku": "FSH-03"},
            {"op": "put", "sku": "SNK-41", "name": "Snack A", "price": 950},
        ]
        job = self.app.post("/stores/busan/imports", edits).body["job"]
        run_until_idle(self.app)
        status = self.app.get(f"/jobs/{job}").body
        self.assertEqual(status["state"], "failed")
        self.assertTrue(any("same name" in p for p in status["problems"]))
        self.assertEqual(listing(self.app, "busan"), before)

    def test_unknown_store(self):
        self.assertEqual(self.app.post("/stores/jeju/imports", [{"op": "delete", "sku": "A-1"}]).status, 404)
        self.assertEqual(self.app.post("/stores/busan/imports", []).status, 400)
