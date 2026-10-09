"""End-to-end: sign-up -> identity-check run -> member status, notices, admin backlog."""
import unittest

from tests.support import VendorHome, app_with_members, load_signups

# sandbox personas whose details the vendor reports as not matching
SANDBOX_MISMATCH = {"김하늘", "오지훈", "임하린", "황보 현"}


class OnboardingRunTest(unittest.TestCase):
    def setUp(self):
        self.home = VendorHome()
        self.signups = load_signups()
        self.app, self.ids = app_with_members(self.home, self.signups)
        # noise: a second sign-up with an already registered phone is refused
        dup = dict(self.signups[3], name="최민준", phone="010 5502 9913")
        self.assertEqual(self.app.post("/members", dup).status, 409)
        self.run = self.app.post("/kyc/run")

    def tearDown(self):
        self.home.cleanup()

    def member(self, member_id):
        resp = self.app.get(f"/members/{member_id}")
        self.assertEqual(resp.status, 200, resp.body)
        return resp.body

    def test_run_sends_every_new_member(self):
        self.assertEqual(self.run.status, 200, self.run.body)
        self.assertEqual(self.run.body["sent"], len(self.signups))
        self.assertTrue(self.run.body["jobs"])

    def test_no_member_is_left_waiting_after_the_run(self):
        waiting = [i for i in self.ids if self.member(i)["status"] == "pending"]
        self.assertEqual(waiting, [], "new members are still waiting for the identity check")

    def test_decisions_follow_the_identity_check(self):
        for member_id, payload in zip(self.ids, self.signups):
            body = self.member(member_id)
            expected = "rejected" if body["name"] in SANDBOX_MISMATCH else "active"
            self.assertEqual(body["status"], expected, f"{member_id} {payload['name']}")

    def test_backlog_report_is_empty(self):
        report = self.app.get("/reports/kyc-backlog").body
        self.assertEqual(report["total"], len(self.signups))
        self.assertEqual(report["counts"]["pending"], 0, "admin backlog still lists pending members")
        self.assertEqual(report["pending"], [])

    def test_every_member_gets_exactly_one_notice(self):
        messages = self.app.get("/outbox").body["items"]
        self.assertEqual(sorted(m["member"] for m in messages), sorted(self.ids),
                         "some members never got a sign-up result message")

    def test_next_run_has_nothing_new_to_send(self):
        again = self.app.post("/kyc/run").body
        self.assertEqual(again["sent"], 0)
        self.assertEqual(again["jobs"], [])

    def test_member_detail_masks_phone(self):
        body = self.member(self.ids[0])
        self.assertEqual(body["phone"], "010-****-7730")
        self.assertEqual(body["channel"], "app")


if __name__ == "__main__":
    unittest.main()
