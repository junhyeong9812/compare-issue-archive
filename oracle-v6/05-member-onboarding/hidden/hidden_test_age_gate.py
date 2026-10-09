"""Hidden judge for the teen sign-up request: teenagers can sign up and get a
final result, but nobody under 19 on the run date ends up as an active member.
Verdicts are read straight from the vendor CLI."""
import datetime
import os
import shutil
import subprocess
import tempfile
import unittest

from hbonboard.app import build_app

HERE = os.path.dirname(os.path.abspath(__file__))
IDCHK = os.path.join(HERE, "vendor", "bin", "idchk")
TODAY = datetime.date(2026, 11, 20)
ADULT = {1: "active", 2: "rejected"}

FAMILY = ["김", "이", "박", "최", "정", "강", "조", "윤", "장", "임", "한", "오"]
GIVEN = ["하율", "서아", "지호", "은우", "도아", "선우", "예서", "준서", "로아", "이준"]


def person(i, birth, seed=0):
    k = seed * 31 + i
    return {
        "name": FAMILY[k % len(FAMILY)] + GIVEN[(k * 3) % len(GIVEN)],
        "birth": birth,
        "phone": f"010-{5000 + (k * 53) % 4000:04d}-{(k * 6007 + 11) % 10000:04d}",
        "channel": ("app", "web", "partner")[k % 3],
    }


def teens(count, seed):
    out = []
    for i in range(count):
        age = 14 + i % 5                       # 14..18 on TODAY
        month = 1 + (i * 5) % 12
        day = 1 + (i * 7) % 28
        year = TODAY.year - age
        if (month, day) > (TODAY.month, TODAY.day):
            year -= 1                           # birthday later in the year: one year older
            if TODAY.year - year - 1 > 18:
                year += 1
                month, day = TODAY.month, TODAY.day - 1
        out.append(person(i, f"{year}-{month:02d}-{day:02d}", seed))
    return out


def age_on(birth, today):
    b = datetime.date.fromisoformat(birth)
    return today.year - b.year - ((today.month, today.day) < (b.month, b.day))


def vendor_verdict(home, ref):
    env = dict(os.environ, IDCHK_HOME=home)
    proc = subprocess.run([IDCHK, "result", ref], capture_output=True, text=True, env=env)
    if proc.returncode != 0:
        return None
    fields = dict(line.split(" ", 1) for line in proc.stdout.splitlines())
    return int(fields["v"])


class AgeGateJudge(unittest.TestCase):
    def setUp(self):
        self.home = tempfile.mkdtemp(prefix="hidden-idchk-")
        self.app = build_app(today=TODAY, idchk_home=self.home)

    def tearDown(self):
        shutil.rmtree(self.home, ignore_errors=True)

    def register(self, payloads):
        ids = []
        for payload in payloads:
            resp = self.app.post("/members", payload)
            self.assertEqual(resp.status, 201, (payload, resp.body))
            ids.append(resp.body["id"])
        return ids

    def run_once(self):
        resp = self.app.post("/kyc/run")
        self.assertEqual(resp.status, 200, resp.body)
        return resp.body

    def judge(self, ids):
        verified_minors = 0
        notices = self.app.get("/outbox").body["items"]
        for member_id in ids:
            body = self.app.get(f"/members/{member_id}").body
            verdict = vendor_verdict(self.home, member_id)
            self.assertIsNotNone(verdict, f"the vendor never checked {member_id}")
            age = age_on(body["birth"], TODAY)
            status = body["status"]
            if age >= 19:
                self.assertEqual(status, ADULT[verdict], f"adult {member_id} age {age}")
            elif verdict == 2:
                self.assertEqual(status, "rejected", f"minor {member_id} age {age}")
            else:
                verified_minors += 1
                self.assertNotEqual(status, "active",
                                    f"{member_id} is {age} on the run date but became an active member")
                self.assertNotIn(status, ("pending", "review", "rejected"),
                                 f"verified minor {member_id} has no usable result: {status}")
            mine = [m for m in notices if m["member"] == member_id]
            self.assertEqual(len(mine), 1, f"{member_id} notices {mine}")
        report = self.app.get("/reports/kyc-backlog").body
        self.assertEqual(report["counts"]["pending"], 0)
        self.assertEqual(report["counts"].get("active", 0),
                         sum(1 for i in ids if self.app.get(f"/members/{i}").body["status"] == "active"))
        return verified_minors

    def test_teen_day(self):
        ids = self.register(teens(20, 1))
        self.assertEqual(self.run_once()["sent"], 20)
        self.assertGreater(self.judge(ids), 0)

    def test_age_boundaries(self):
        refused = self.app.post("/members", person(90, "2012-11-21"))
        self.assertEqual(refused.status, 400, "13-year-old must be refused")
        payloads = [
            person(91, "2012-11-20"),   # 14 today
            person(92, "2007-11-21"),   # 18, turns 19 tomorrow
            person(93, "2007-11-20"),   # 19 today
            person(94, "1980-02-02"),
        ]
        ids = self.register(payloads)
        self.run_once()
        self.judge(ids)

    def test_mixed_day_and_rerun(self):
        mixed = teens(9, 2) + [person(40 + i, f"{1970 + i * 3}-0{1 + i % 9}-1{i % 9}", 2) for i in range(9)]
        ids = self.register(mixed)
        self.run_once()
        minors = self.judge(ids)
        before = {i: self.app.get(f"/members/{i}").body["status"] for i in ids}
        again = self.run_once()
        self.assertEqual(again["sent"], 0)
        after = {i: self.app.get(f"/members/{i}").body["status"] for i in ids}
        self.assertEqual(before, after)
        self.assertGreater(minors, 0)


if __name__ == "__main__":
    unittest.main()
