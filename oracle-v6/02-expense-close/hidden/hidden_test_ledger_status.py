"""Hidden judge for the ledger status request.

Every vendor CLI call made by the service goes through a recording wrapper
(settings.ledger_bin). The service must answer ledger_status from what the
ledger lists, and must not make a single per-entry lookup (`show`) on the
way: not while settling, not while serving claim detail or claim lists."""
import json
import os
import shutil
import stat
import tempfile
import unittest

from hbexpense.app import build_app

HERE = os.path.dirname(os.path.abspath(__file__))
LEDGERCTL = os.path.join(HERE, "vendor", "bin", "ledgerctl")

OCTOBER = [
    {"claimant": "Kim Minseo", "cost_center": "CC520", "category": "TRAINING", "amount": 450000,
     "spent_on": "2026-10-07", "description": "정보보호 관리체계 인증 심사원 과정"},
    {"claimant": "Jo Ara", "cost_center": "CC410", "category": "COMMS", "amount": 33000,
     "spent_on": "2026-10-15", "description": "VPN token"},
]
NOVEMBER = [
    {"claimant": "백지호", "cost_center": "CC520", "category": "SUPPLIES", "amount": 18000,
     "spent_on": "2026-11-20", "description": "케이블 정리함"},
    {"claimant": "남궁민수", "cost_center": "CC520", "category": "TRAVEL", "amount": 812000,
     "spent_on": "2026-11-12", "description": "싱가포르 핀테크 페스티벌 참관 항공권 및 숙박 일체"},
]


class LedgerStatusJudge(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.mkdtemp(prefix="hidden-status-")
        self.home = os.path.join(self.tmp, "journal")
        os.mkdir(self.home)
        self.calls = os.path.join(self.tmp, "calls.log")
        self.wrapper = os.path.join(self.tmp, "ledgerctl")
        with open(self.wrapper, "w") as f:
            f.write('#!/bin/sh\nprintf "%%s\\n" "$1" >> "%s"\nexec "%s" "$@"\n' % (self.calls, LEDGERCTL))
        os.chmod(self.wrapper, os.stat(self.wrapper).st_mode | stat.S_IEXEC)

    def tearDown(self):
        shutil.rmtree(self.tmp, ignore_errors=True)

    def vendor_calls(self, command):
        if not os.path.exists(self.calls):
            return 0
        with open(self.calls) as f:
            return sum(1 for line in f if line.strip() == command)

    def app(self, **overrides):
        return build_app(ledger_home=self.home, ledger_bin=self.wrapper, **overrides)

    def submit(self, app, claims, approve=True):
        ids = []
        for payload in claims:
            resp = app.post("/claims", payload)
            self.assertEqual(resp.status, 201, resp.body)
            ids.append(resp.body["id"])
            if approve:
                self.assertEqual(app.post(f"/claims/{ids[-1]}/approve").status, 200)
        return ids

    def settle(self, app, month):
        run = app.post("/settlement/run", query={"month": month})
        self.assertEqual(run.status, 200, run.body)
        self.assertEqual(run.body["failed"], [])
        return run.body

    def assert_statuses(self, app, expected):
        for cid, want in expected.items():
            resp = app.get(f"/claims/{cid}")
            self.assertEqual(resp.status, 200, resp.body)
            self.assertEqual(resp.body.get("ledger_status", "MISSING"), want, f"detail of {cid}")
        resp = app.get("/claims")
        self.assertEqual(resp.status, 200, resp.body)
        listed = {c["id"]: c.get("ledger_status", "MISSING") for c in resp.body["items"]}
        for cid, want in expected.items():
            self.assertEqual(listed.get(cid, "MISSING"), want, f"list entry of {cid}")
        resp = app.get("/claims", {"status": "posted"})
        self.assertEqual(resp.status, 200, resp.body)
        for c in resp.body["items"]:
            self.assertEqual(c.get("ledger_status", "MISSING"), expected[c["id"]], f"posted list {c['id']}")

    def test_parked_entries_show_as_not_booked_without_per_entry_lookups(self):
        # a deployment where memos are not clipped to the ledger's limit: the
        # long Hangul memos are parked by the ledger (receipt, but not listed)
        with open(os.path.join(HERE, "tests", "fixtures", "claims_2026_09.json"), encoding="utf-8") as f:
            claims = json.load(f)
        app = self.app(ledger_memo_bytes=200)
        ids = self.submit(app, claims)
        rejected = self.submit(app, [dict(claims[0], description="반려 대상")], approve=False)[0]
        app.post(f"/claims/{rejected}/reject")
        waiting = self.submit(app, [dict(claims[1], description="승인 대기")], approve=False)[0]
        approved_only = self.submit(app, [dict(claims[2], spent_on="2026-10-02")])[0]
        self.settle(app, "2026-09")
        parked = {ids[3], ids[5], ids[9]}
        expected = {cid: ("not_booked" if cid in parked else "booked") for cid in ids}
        expected.update({rejected: None, waiting: None, approved_only: None})
        self.assert_statuses(app, expected)
        self.assert_statuses(app, expected)  # asked again: same answer
        report = app.get("/reports/monthly", {"month": "2026-09"}).body
        self.assertEqual(report["entries"], 7)
        self.assertEqual(self.vendor_calls("show"), 0, "per-entry ledger lookups (show) were made")

    def test_several_months_and_default_settings(self):
        app = self.app()
        oct_ids = self.submit(app, OCTOBER)
        nov_ids = self.submit(app, NOVEMBER)
        self.settle(app, "2026-10")
        expected = {cid: "booked" for cid in oct_ids}
        expected.update({cid: None for cid in nov_ids})
        self.assert_statuses(app, expected)
        self.settle(app, "2026-11")
        expected.update({cid: "booked" for cid in nov_ids})
        self.assert_statuses(app, expected)
        for cid in oct_ids + nov_ids:
            self.assertEqual(app.get(f"/claims/{cid}").body["status"], "posted")
        self.assertEqual(self.vendor_calls("show"), 0, "per-entry ledger lookups (show) were made")
        self.assertGreater(self.vendor_calls("post"), 0, "service did not go through settings.ledger_bin")


if __name__ == "__main__":
    unittest.main()
