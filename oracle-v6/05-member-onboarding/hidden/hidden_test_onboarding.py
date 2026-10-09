"""Hidden judge: after one identity-check run every new member must hold the
vendor's own verdict (checked straight through the vendor CLI, not only
through our API), for other sign-up volumes and the sample day."""
import datetime
import json
import os
import shutil
import subprocess
import tempfile
import unittest

from hbonboard.app import build_app

HERE = os.path.dirname(os.path.abspath(__file__))
IDCHK = os.path.join(HERE, "vendor", "bin", "idchk")
TODAY = datetime.date(2026, 11, 20)

FAMILY = ["김", "이", "박", "최", "정", "강", "조", "윤", "장", "임", "한", "오", "서", "신", "권"]
GIVEN = ["민지", "서윤", "도현", "하준", "지아", "수빈", "예준", "유진", "시우", "채은", "건우", "나연"]
STATUS = {1: "active", 2: "rejected"}


def signups(count, seed):
    out = []
    for i in range(count):
        k = seed * 97 + i
        name = FAMILY[k % len(FAMILY)] + GIVEN[(k * 7) % len(GIVEN)]
        if i % 9 == 4:
            name = f"Jung Mina{i}"
        year = 1960 + (k * 13) % 45
        out.append({
            "name": name,
            "birth": f"{year}-{1 + k % 12:02d}-{1 + (k * 5) % 28:02d}",
            "phone": f"010-{3000 + (k * 37) % 6000:04d}-{(k * 7919) % 10000:04d}",
            "channel": ("app", "web", "partner")[k % 3],
        })
    return out


def vendor_verdict(home, ref):
    env = dict(os.environ, IDCHK_HOME=home)
    proc = subprocess.run([IDCHK, "result", ref], capture_output=True, text=True, env=env)
    if proc.returncode != 0:
        return None
    fields = dict(line.split(" ", 1) for line in proc.stdout.splitlines())
    return int(fields["v"])


class OnboardingJudge(unittest.TestCase):
    def setUp(self):
        self.home = tempfile.mkdtemp(prefix="hidden-idchk-")
        self.app = build_app(today=TODAY, idchk_home=self.home)

    def tearDown(self):
        shutil.rmtree(self.home, ignore_errors=True)

    def register(self, payloads):
        ids = []
        for payload in payloads:
            resp = self.app.post("/members", payload)
            self.assertEqual(resp.status, 201, resp.body)
            ids.append(resp.body["id"])
        return ids

    def run_once(self):
        resp = self.app.post("/kyc/run")
        self.assertEqual(resp.status, 200, resp.body)
        return resp.body

    def check(self, ids):
        rejected = 0
        for member_id in ids:
            verdict = vendor_verdict(self.home, member_id)
            self.assertIsNotNone(verdict, f"the vendor never checked {member_id}")
            body = self.app.get(f"/members/{member_id}").body
            self.assertEqual(body["status"], STATUS[verdict], member_id)
            rejected += verdict == 2
        notices = self.app.get("/outbox").body["items"]
        self.assertEqual(sorted(m["member"] for m in notices), sorted(ids))
        report = self.app.get("/reports/kyc-backlog").body
        self.assertEqual(report["counts"]["pending"], 0)
        return rejected

    def test_large_day(self):
        ids = self.register(signups(30, 1))
        self.assertEqual(self.run_once()["sent"], 30)
        self.assertGreater(self.check(ids), 0)

    def test_exactly_twelve(self):
        ids = self.register(signups(12, 2))
        self.run_once()
        self.check(ids)

    def test_thirteen(self):
        ids = self.register(signups(13, 3))
        self.run_once()
        self.check(ids)

    def test_two_days_in_a_row(self):
        first = self.register(signups(7, 4))
        self.run_once()
        second = self.register(signups(18, 5))
        body = self.run_once()
        self.assertEqual(body["sent"], 18)
        self.check(first + second)

    def test_sample_day(self):
        with open(os.path.join(HERE, "tests", "fixtures", "signups_2026_10.json"), encoding="utf-8") as f:
            payloads = json.load(f)
        ids = self.register(payloads)
        self.run_once()
        self.assertEqual(self.check(ids), 4)
        self.assertEqual(self.run_once()["sent"], 0)


if __name__ == "__main__":
    unittest.main()
