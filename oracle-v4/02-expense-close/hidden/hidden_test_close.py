"""Hidden judge: every settled claim must be booked in the ledger itself
(checked straight through the vendor CLI, not only through our report),
with the longest memo the ledger books, for another month and the sample month."""
import json
import os
import shutil
import subprocess
import tempfile
import unittest

from hbexpense.app import build_app

HERE = os.path.dirname(os.path.abspath(__file__))
LEDGERCTL = os.path.join(HERE, "vendor", "bin", "ledgerctl")

NOVEMBER = [
    # exactly at the edge, pure Hangul after the prefix
    {"claimant": "강다은", "cost_center": "CC410", "category": "MEAL", "amount": 31000,
     "spent_on": "2026-11-03", "description": "협력사미팅점심식대"},
    # one byte over the edge
    {"claimant": "강다은", "cost_center": "CC410", "category": "MEAL", "amount": 28000,
     "spent_on": "2026-11-04", "description": "협력사미팅점심식대A"},
    # 4-byte characters straddling the edge
    {"claimant": "서준", "cost_center": "CC410", "category": "MEAL", "amount": 64000,
     "spent_on": "2026-11-05", "description": "팀 회식 피자값 🍕🍕🍕"},
    # long ASCII claimant + Hangul
    {"claimant": "Kim Minseo", "cost_center": "CC520", "category": "TRAINING", "amount": 450000,
     "spent_on": "2026-11-07", "description": "정보보호 관리체계 인증 심사원 과정"},
    # long, cut by the 40-character rule as well
    {"claimant": "남궁민수", "cost_center": "CC520", "category": "TRAVEL", "amount": 812000,
     "spent_on": "2026-11-12", "description": "싱가포르 핀테크 페스티벌 참관 항공권 및 숙박 일체"},
    # short ones stay untouched
    {"claimant": "Jo Ara", "cost_center": "CC410", "category": "COMMS", "amount": 33000,
     "spent_on": "2026-11-15", "description": "VPN token"},
    {"claimant": "백지호", "cost_center": "CC520", "category": "SUPPLIES", "amount": 18000,
     "spent_on": "2026-11-20", "description": "케이블 정리함"},
    # space right at the edge
    {"claimant": "임하린", "cost_center": "CC410", "category": "SUPPLIES", "amount": 77000,
     "spent_on": "2026-11-30", "description": "토너 4개 및 A4 용지 2박스 구매"},
]


def fit(text, limit):
    out, used = "", 0
    for ch in text:
        n = len(ch.encode("utf-8"))
        if used + n > limit:
            break
        out += ch
        used += n
    return out


def acceptable_memos(full):
    chars40 = full[:40].rstrip() if len(full) > 40 else full
    return {fit(chars40, 48).rstrip(), fit(full, 48).rstrip()}


def ledger_rows(home, date_from, date_to):
    env = dict(os.environ, LEDGERCTL_HOME=home)
    out = subprocess.run([LEDGERCTL, "list", "--from", date_from, "--to", date_to],
                         capture_output=True, text=True, env=env, check=True).stdout
    rows = {}
    for line in out.splitlines():
        receipt, date, acct, amt, ref, memo = line.split("\t", 5)
        rows[ref] = {"receipt": receipt, "amount": int(amt), "memo": memo}
    return rows


class CloseJudge(unittest.TestCase):
    def setUp(self):
        self.home = tempfile.mkdtemp(prefix="hidden-ledger-")

    def tearDown(self):
        shutil.rmtree(self.home, ignore_errors=True)

    def settle(self, claims, month):
        app = build_app(ledger_home=self.home)
        ids = []
        for payload in claims:
            resp = app.post("/claims", payload)
            self.assertEqual(resp.status, 201, resp.body)
            ids.append(resp.body["id"])
            app.post(f"/claims/{ids[-1]}/approve")
        run = app.post("/settlement/run", query={"month": month})
        self.assertEqual(run.status, 200, run.body)
        self.assertEqual(run.body["failed"], [])
        return app, ids

    def check_month(self, claims, month, last_day):
        app, ids = self.settle(claims, month)
        rows = ledger_rows(self.home, f"{month}-01", f"{month}-{last_day}")
        self.assertEqual(sorted(rows), sorted(ids), "ledger does not hold every settled claim")
        for cid, claim in zip(ids, claims):
            full = f"{cid} {claim['claimant']} {claim['description']}"
            self.assertEqual(rows[cid]["amount"], claim["amount"])
            self.assertIn(rows[cid]["memo"].rstrip(), acceptable_memos(full),
                          f"ledger memo for {cid} is not the longest bookable memo")
            detail = app.get(f"/claims/{cid}").body
            self.assertEqual(detail["receipt"], rows[cid]["receipt"])
        report = app.get("/reports/monthly", {"month": month}).body
        self.assertEqual(report["entries"], len(claims))
        self.assertEqual(report["total"], sum(c["amount"] for c in claims))
        for line in report["lines"]:
            self.assertEqual(line["memo"], rows[line["ref"]]["memo"])
            self.assertEqual(line["receipt"], rows[line["ref"]]["receipt"])

    def test_november_close(self):
        self.check_month(NOVEMBER, "2026-11", "30")

    def test_sample_september_close(self):
        with open(os.path.join(HERE, "tests", "fixtures", "claims_2026_09.json"), encoding="utf-8") as f:
            claims = json.load(f)
        self.check_month(claims, "2026-09", "30")

    def test_memos_that_already_fit_are_untouched(self):
        app, ids = self.settle(NOVEMBER, "2026-11")
        rows = ledger_rows(self.home, "2026-11-01", "2026-11-30")
        self.assertEqual(rows[ids[0]]["memo"], f"{ids[0]} 강다은 협력사미팅점심식대")
        self.assertEqual(rows[ids[5]]["memo"], f"{ids[5]} Jo Ara VPN token")
        self.assertEqual(rows[ids[6]]["memo"], f"{ids[6]} 백지호 케이블 정리함")


if __name__ == "__main__":
    unittest.main()
