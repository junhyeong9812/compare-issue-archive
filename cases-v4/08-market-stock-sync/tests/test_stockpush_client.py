"""Contract checks against the vendor CLI binary shipped in vendor/bin."""
import unittest

from hbstock.config import DEFAULTS
from hbstock.market.client import StockpushClient
from hbstock.models import VendorError
from tests.support import VendorHome


class StockpushClientTest(unittest.TestCase):
    def setUp(self):
        self.home = VendorHome()
        self.client = StockpushClient(DEFAULTS.with_overrides(stockpush_home=self.home.path))

    def tearDown(self):
        self.home.cleanup()

    def test_push_then_rows_roundtrip(self):
        transfer = self.client.push(["GD-101\t5", "VC-2001\t12"])
        self.assertTrue(transfer.transfer_id.startswith("T"))
        self.assertEqual(transfer.lines, 2)
        rows = {r.sku: r.qty for r in self.client.rows()}
        self.assertEqual(rows, {"GD-101": 5, "VC-2001": 12})

    def test_later_push_overwrites_quantity(self):
        self.client.push(["GD-101\t5"])
        second = self.client.push(["GD-101\t8"])
        row = self.client.show("GD-101")
        self.assertEqual(row.qty, 8)
        self.assertEqual(row.transfer, second.transfer_id)

    def test_bad_lines_are_refused(self):
        with self.assertRaises(VendorError) as ctx:
            self.client.push(["GD-101\t2.5"])
        self.assertEqual(ctx.exception.code, "E15")
        with self.assertRaises(VendorError) as ctx:
            self.client.show("GD-999")
        self.assertEqual(ctx.exception.code, "E31")


if __name__ == "__main__":
    unittest.main()
