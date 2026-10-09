"""Contract checks against the vendor CLI binary shipped in vendor/bin."""
import unittest

from hbonboard.config import DEFAULTS
from hbonboard.kyc.client import IdchkClient
from hbonboard.models import KycRequest, VendorError
from tests.support import VendorHome


class IdchkClientTest(unittest.TestCase):
    def setUp(self):
        self.home = VendorHome()
        self.client = IdchkClient(DEFAULTS.with_overrides(idchk_home=self.home.path))

    def tearDown(self):
        self.home.cleanup()

    def test_submit_then_result_roundtrip(self):
        ticket = self.client.submit([
            KycRequest("T-1", "홍길동", "19900101", "01011112222"),
            KycRequest("T-2", "Kim Ara", "19851231", "01033334444"),
            KycRequest("T-3", "이도", "20000229", "0162223333"),
        ])
        self.assertTrue(ticket.job_id.startswith("J"))
        self.assertEqual(ticket.lines, 3)
        for ref in ("T-1", "T-2", "T-3"):
            self.assertIn(self.client.result(ref), (1, 2))
        self.assertEqual(self.client.job(ticket.job_id)["in"], "3")

    def test_unknown_reference_has_no_result(self):
        self.assertIsNone(self.client.result("T-404"))

    def test_malformed_line_rejects_the_file(self):
        with self.assertRaises(VendorError) as ctx:
            self.client.submit([KycRequest("T-1", "홍길동", "19900101", "021234567")])
        self.assertEqual(ctx.exception.code, "E17")
        self.assertIsNone(self.client.result("T-1"))


if __name__ == "__main__":
    unittest.main()
