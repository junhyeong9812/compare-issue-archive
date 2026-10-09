import unittest

from hbstock.config import DEFAULTS
from hbstock.market.feed import build_feed, feed_line, market_qty
from hbstock.models import Product
from hbstock.util.sku import is_sku, normalize_sku, parse_qty


class FeedTest(unittest.TestCase):
    def test_market_qty_keeps_safety_units(self):
        self.assertEqual(market_qty(Product("GD-1", "a", "goods", 1, on_hand=10, reserved=2), DEFAULTS), 7)
        self.assertEqual(market_qty(Product("GD-1", "a", "goods", 1, on_hand=3, reserved=0), DEFAULTS), 2)

    def test_market_qty_never_negative(self):
        self.assertEqual(market_qty(Product("GD-1", "a", "goods", 1, on_hand=1, reserved=4), DEFAULTS), 0)

    def test_feed_line(self):
        self.assertEqual(feed_line("GD-104", 5), "GD-104\t5")
        self.assertEqual(feed_line("VC-2001", "12"), "VC-2001\t12")

    def test_build_feed_reports_announced_quantities(self):
        products = [Product("GD-101", "a", "goods", 1, on_hand=4), Product("GD-102", "b", "goods", 1, on_hand=9)]
        lines, sent = build_feed(products, DEFAULTS)
        self.assertEqual(len(lines), 2)
        self.assertEqual(sent, {"GD-101": 3, "GD-102": 8})


class SkuTest(unittest.TestCase):
    def test_normalize_and_check(self):
        self.assertEqual(normalize_sku(" vc_2001 "), "VC-2001")
        self.assertTrue(is_sku("DG-40312"))
        self.assertFalse(is_sku("DG-40"))

    def test_parse_qty(self):
        self.assertEqual(parse_qty("03"), 3)
        self.assertEqual(parse_qty(0, allow_zero=True), 0)
        for bad in ("-1", "2.5", None, 0):
            with self.assertRaises(ValueError):
                parse_qty(bad)


if __name__ == "__main__":
    unittest.main()
