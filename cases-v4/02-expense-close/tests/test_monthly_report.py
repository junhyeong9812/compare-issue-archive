"""End-to-end: submit -> approve -> month-end settlement -> monthly report."""
import unittest

from tests.support import LedgerHome, app_with_claims, load_claims


class MonthEndCloseTest(unittest.TestCase):
    def setUp(self):
        self.home = LedgerHome()
        self.claims = load_claims()
        self.app, self.ids = app_with_claims(self.home, self.claims)
        # noise: a rejected claim and a claim from the next month
        extra = dict(self.claims[0], description="반려 대상 택시비", amount=12000)
        rejected = self.app.post("/claims", extra).body["id"]
        self.app.post(f"/claims/{rejected}/reject")
        october = dict(self.claims[6], spent_on="2026-10-01")
        oid = self.app.post("/claims", october).body["id"]
        self.app.post(f"/claims/{oid}/approve")
        self.run = self.app.post("/settlement/run", query={"month": "2026-09"})

    def tearDown(self):
        self.home.cleanup()

    def report(self):
        resp = self.app.get("/reports/monthly", {"month": "2026-09"})
        self.assertEqual(resp.status, 200, resp.body)
        return resp.body

    def test_settlement_gives_every_approved_claim_a_receipt(self):
        self.assertEqual(self.run.status, 200, self.run.body)
        self.assertEqual(self.run.body["failed"], [])
        self.assertEqual(sorted(p["id"] for p in self.run.body["posted"]), sorted(self.ids))

    def test_claim_detail_shows_posted_status(self):
        body = self.app.get(f"/claims/{self.ids[3]}").body
        self.assertEqual(body["status"], "posted")
        self.assertTrue(body["receipt"].startswith("R"))

    def test_monthly_report_lists_every_settled_claim(self):
        refs = sorted(line["ref"] for line in self.report()["lines"])
        self.assertEqual(
            refs, sorted(self.ids),
            "settled September claims are missing from the month-end report",
        )

    def test_monthly_report_totals_match_settlement(self):
        report = self.report()
        expected = sum(c["amount"] for c in self.claims)
        self.assertEqual(report["entries"], len(self.claims), "entry count is short")
        self.assertEqual(report["total"], expected, "month-end total is lower than what was settled")
        per_center = {}
        for c in self.claims:
            per_center[c["cost_center"]] = per_center.get(c["cost_center"], 0) + c["amount"]
        self.assertEqual(report["by_cost_center"], per_center)

    def test_report_memos_stay_readable(self):
        memos = {line["ref"]: line["memo"] for line in self.report()["lines"]}
        expected_full = {
            self.ids[0]: f"{self.ids[0]} 김하늘 부산 출장 KTX 왕복",
            self.ids[2]: f"{self.ids[2]} 이서연 사무용품 구매 토너",
            self.ids[4]: f"{self.ids[4]} 정유나 9월 휴대폰 요금",
            self.ids[6]: f"{self.ids[6]} 김하늘 야근 식대",
            self.ids[7]: f"{self.ids[7]} 오지훈 SQL 분석 세미나 참가",
            self.ids[8]: f"{self.ids[8]} 윤소희 모니터 암 2개 구매",
        }
        for ref, memo in expected_full.items():
            self.assertEqual(memos.get(ref), memo)
        self.assertEqual(memos.get(self.ids[1]), f"{self.ids[1]} Park Jiwon Team dinner with p")
        for ref, memo in memos.items():
            self.assertTrue(memo.startswith(ref + " "), memo)

    def test_text_report_renders(self):
        text = self.app.get("/reports/monthly.txt", {"month": "2026-09"}).body["text"]
        self.assertTrue(text.startswith("Expense report 2026-09"))
        self.assertIn("entries", text.splitlines()[-1])

    def test_october_claim_is_not_settled_in_september(self):
        refs = {line["ref"] for line in self.report()["lines"]}
        self.assertFalse(any(r.startswith("EX-2610") for r in refs))


if __name__ == "__main__":
    unittest.main()
