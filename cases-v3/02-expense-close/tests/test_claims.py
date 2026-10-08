import unittest

from hbexpense.app import build_app
from hbexpense.models import Conflict


def payload(**kw):
    base = {
        "claimant": "김하늘", "cost_center": "cc210", "category": "travel",
        "amount": "15000", "spent_on": "2026-09-01", "description": "  택시비\t(야근) ",
    }
    base.update(kw)
    return base


class ClaimValidationTest(unittest.TestCase):
    def setUp(self):
        self.app = build_app(ledger_home="/nonexistent")

    def test_submit_normalizes_fields(self):
        resp = self.app.post("/claims", payload())
        self.assertEqual(resp.status, 201)
        self.assertEqual(resp.body["description"], "택시비 (야근)")
        self.assertEqual(resp.body["cost_center"], "CC210")
        self.assertEqual(resp.body["category"], "TRAVEL")
        self.assertEqual(resp.body["amount"], 15000)
        self.assertEqual(resp.body["id"], "EX-2609001")

    def test_description_over_screen_limit_is_rejected(self):
        resp = self.app.post("/claims", payload(description="가" * 31))
        self.assertEqual(resp.status, 400)
        self.assertEqual(self.app.post("/claims", payload(description="가" * 30)).status, 201)

    def test_bad_cost_center_and_category(self):
        self.assertEqual(self.app.post("/claims", payload(cost_center="210")).status, 400)
        self.assertEqual(self.app.post("/claims", payload(category="GIFT")).status, 400)

    def test_amount_bounds(self):
        self.assertEqual(self.app.post("/claims", payload(amount=0)).status, 400)
        self.assertEqual(self.app.post("/claims", payload(amount="many")).status, 400)
        self.assertEqual(self.app.post("/claims", payload(amount=5_000_001)).status, 400)

    def test_approve_twice_conflicts(self):
        cid = self.app.post("/claims", payload()).body["id"]
        self.assertEqual(self.app.post(f"/claims/{cid}/approve").status, 200)
        self.assertEqual(self.app.post(f"/claims/{cid}/approve").status, 409)

    def test_unknown_claim_is_404(self):
        self.assertEqual(self.app.get("/claims/EX-0000000").status, 404)

    def test_bad_month_is_400(self):
        self.assertEqual(self.app.get("/reports/monthly", {"month": "2026-13"}).status, 400)


if __name__ == "__main__":
    unittest.main()
