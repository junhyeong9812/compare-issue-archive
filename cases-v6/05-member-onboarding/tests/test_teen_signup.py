"""Teenagers (14 and over) sign up and get their identity-check result in one run."""
import unittest

from tests.support import VendorHome, app_with_members

TEENS = [
    {"name": "김도윤", "birth": "2012-10-09", "phone": "010-3101-2201", "channel": "app"},
    {"name": "이하은", "birth": "2011-02-17", "phone": "010-3101-2202", "channel": "app"},
    {"name": "박서준", "birth": "2010-07-30", "phone": "010-3101-2203", "channel": "web"},
    {"name": "최지우", "birth": "2009-12-05", "phone": "010-3101-2204", "channel": "app"},
    {"name": "정예린", "birth": "2008-04-21", "phone": "010-3101-2205", "channel": "partner"},
    {"name": "강시우", "birth": "2007-10-10", "phone": "010-3101-2206", "channel": "app"},
    {"name": "조아인", "birth": "2010-01-13", "phone": "010-3101-2207", "channel": "web"},
    {"name": "윤건우", "birth": "2011-08-08", "phone": "010-3101-2208", "channel": "app"},
]
ADULTS = [
    {"name": "한서진", "birth": "1990-05-05", "phone": "010-3101-2301", "channel": "app"},
    {"name": "오민재", "birth": "2007-10-09", "phone": "010-3101-2302", "channel": "web"},
]


class TeenSignupTest(unittest.TestCase):
    def setUp(self):
        self.home = VendorHome()
        self.app, self.ids = app_with_members(self.home, TEENS + ADULTS)
        self.teen_ids = self.ids[: len(TEENS)]
        self.run = self.app.post("/kyc/run")

    def tearDown(self):
        self.home.cleanup()

    def test_thirteen_year_old_is_refused(self):
        resp = self.app.post("/members", {"name": "서하람", "birth": "2012-10-10",
                                          "phone": "010-3101-2299", "channel": "app"})
        self.assertEqual(resp.status, 400)

    def test_run_sends_teenagers_too(self):
        self.assertEqual(self.run.status, 200, self.run.body)
        self.assertEqual(self.run.body["sent"], len(TEENS) + len(ADULTS))

    def test_teenagers_get_a_final_result(self):
        for member_id in self.teen_ids:
            status = self.app.get(f"/members/{member_id}").body["status"]
            self.assertNotIn(status, ("pending", "review"), f"{member_id} has no final result")

    def test_every_teenager_gets_one_result_message(self):
        messages = self.app.get("/outbox").body["items"]
        for member_id in self.teen_ids:
            mine = [m for m in messages if m["member"] == member_id]
            self.assertEqual(len(mine), 1, f"{member_id} messages: {mine}")
            self.assertNotEqual(mine[0]["kind"], "review")

    def test_backlog_is_empty(self):
        report = self.app.get("/reports/kyc-backlog").body
        self.assertEqual(report["counts"]["pending"], 0)
        self.assertEqual(report["counts"]["review"], 0)


if __name__ == "__main__":
    unittest.main()
