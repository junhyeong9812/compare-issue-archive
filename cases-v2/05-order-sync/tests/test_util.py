import unittest

from orderfeed.util import page_token
from orderfeed.util.money import format_amount, split_evenly


class PageTokenTest(unittest.TestCase):
    def test_roundtrip_without_padding(self):
        for offset in (0, 7, 123456):
            text = page_token.encode(page_token.PageToken(offset, 42))
            self.assertNotIn("=", text)
            self.assertEqual(page_token.decode(text).offset, offset)

    def test_negative_offset_rejected(self):
        bad = page_token.encode(page_token.PageToken(-1, 1))
        with self.assertRaises(ValueError):
            page_token.decode(bad)


class MoneyTest(unittest.TestCase):
    def test_format(self):
        self.assertEqual(format_amount(1234567), "1,234,567 KRW")
        self.assertEqual(format_amount(-1050, "USD"), "-10.50 USD")

    def test_split(self):
        self.assertEqual(split_evenly(10, 3), [4, 3, 3])
        self.assertEqual(sum(split_evenly(1001, 7)), 1001)
