"""Contract checks against the vendor CLI binary shipped in vendor/bin."""
import unittest

from hbexpense.config import DEFAULTS
from hbexpense.ledger.client import LedgerClient
from hbexpense.models import LedgerEntry, LedgerError
from tests.support import LedgerHome


class LedgerClientTest(unittest.TestCase):
    def setUp(self):
        self.home = LedgerHome()
        self.client = LedgerClient(DEFAULTS.with_overrides(ledger_home=self.home.path))

    def tearDown(self):
        self.home.cleanup()

    def test_post_then_list_roundtrip(self):
        r1 = self.client.post(LedgerEntry("5110", 1000, "2026-09-01", "T-1", "taxi"))
        r2 = self.client.post(LedgerEntry("5120", 2500, "2026-09-30", "T-2", "lunch"))
        self.client.post(LedgerEntry("5120", 700, "2026-10-01", "T-3", "coffee"))
        rows = self.client.list_rows("2026-09-01", "2026-09-30")
        self.assertEqual([r.receipt for r in rows], [r1, r2])
        self.assertEqual(rows[1].amount, 2500)
        self.assertEqual(rows[0].memo, "taxi")

    def test_show_returns_entry_fields(self):
        rc = self.client.post(LedgerEntry("5130", 4200, "2026-09-02", "T-9", "펜 한 다스"))
        info = self.client.show(rc)
        self.assertEqual(info["receipt"], rc)
        self.assertEqual(info["ref"], "T-9")
        self.assertEqual(info["memo"], "펜 한 다스")

    def test_invalid_date_is_rejected_with_vendor_code(self):
        with self.assertRaises(LedgerError) as ctx:
            self.client.post(LedgerEntry("5110", 1000, "2026/09/01", "T-1", "taxi"))
        self.assertEqual(ctx.exception.code, "E13")


if __name__ == "__main__":
    unittest.main()
