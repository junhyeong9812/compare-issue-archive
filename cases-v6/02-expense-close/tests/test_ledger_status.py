"""Claim detail and list show whether the claim's ledger entry is booked."""
import unittest

from tests.support import LedgerHome, app_with_claims, load_claims


class LedgerStatusTest(unittest.TestCase):
    def setUp(self):
        self.home = LedgerHome()
        self.claims = load_claims()
        self.app, self.ids = app_with_claims(self.home, self.claims)
        pending = dict(self.claims[2], description="복사 용지 추가")
        self.pending = self.app.post("/claims", pending).body["id"]
        run = self.app.post("/settlement/run", query={"month": "2026-09"})
        self.assertEqual(run.status, 200, run.body)

    def tearDown(self):
        self.home.cleanup()

    def test_settled_claims_are_booked(self):
        for cid in self.ids:
            resp = self.app.get(f"/claims/{cid}")
            self.assertEqual(resp.status, 200, resp.body)
            self.assertEqual(resp.body["status"], "posted")
            self.assertEqual(resp.body["ledger_status"], "booked", cid)

    def test_unsettled_claim_has_no_ledger_status(self):
        body = self.app.get(f"/claims/{self.pending}").body
        self.assertEqual(body["status"], "submitted")
        self.assertIsNone(body["ledger_status"])

    def test_list_carries_ledger_status(self):
        resp = self.app.get("/claims")
        self.assertEqual(resp.status, 200, resp.body)
        status = {c["id"]: c["ledger_status"] for c in resp.body["items"]}
        self.assertEqual(status[self.pending], None)
        for cid in self.ids:
            self.assertEqual(status[cid], "booked", cid)


if __name__ == "__main__":
    unittest.main()
