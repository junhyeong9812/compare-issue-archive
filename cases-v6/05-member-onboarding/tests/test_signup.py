import unittest

from hbonboard.app import build_app
from tests.support import TODAY


def payload(**kw):
    base = {"name": " 김 하늘 ", "birth": "1991-03-14", "phone": "010-4821-7730", "channel": "APP"}
    base.update(kw)
    return base


class SignupValidationTest(unittest.TestCase):
    def setUp(self):
        self.app = build_app(today=TODAY, idchk_home="/nonexistent")

    def test_register_normalizes_fields(self):
        resp = self.app.post("/members", payload())
        self.assertEqual(resp.status, 201)
        self.assertEqual(resp.body["id"], "M2610001")
        self.assertEqual(resp.body["name"], "김 하늘")
        self.assertEqual(resp.body["phone"], "010-****-7730")
        self.assertEqual(resp.body["channel"], "app")
        self.assertEqual(resp.body["status"], "pending")
        self.assertEqual(resp.body["kyc"], {"state": "new", "job": ""})

    def test_compact_birth_date_is_accepted(self):
        resp = self.app.post("/members", payload(birth="19910314"))
        self.assertEqual(resp.body["birth"], "1991-03-14")

    def test_under_fourteen_is_refused(self):
        self.assertEqual(self.app.post("/members", payload(birth="2012-10-10")).status, 400)
        self.assertEqual(self.app.post("/members", payload(birth="2012-10-09")).status, 201)

    def test_teenagers_can_sign_up(self):
        resp = self.app.post("/members", payload(birth="2009-05-01"))
        self.assertEqual(resp.status, 201, resp.body)
        self.assertEqual(resp.body["status"], "pending")

    def test_bad_phone_and_channel(self):
        self.assertEqual(self.app.post("/members", payload(phone="02-123-4567")).status, 400)
        self.assertEqual(self.app.post("/members", payload(channel="kiosk")).status, 400)

    def test_same_phone_twice_conflicts(self):
        self.assertEqual(self.app.post("/members", payload()).status, 201)
        self.assertEqual(self.app.post("/members", payload(phone="+82 10 4821 7730")).status, 409)

    def test_name_length(self):
        self.assertEqual(self.app.post("/members", payload(name="가" * 21)).status, 400)
        self.assertEqual(self.app.post("/members", payload(name="   ")).status, 400)

    def test_unknown_member_is_404(self):
        self.assertEqual(self.app.get("/members/M0000000").status, 404)


if __name__ == "__main__":
    unittest.main()
