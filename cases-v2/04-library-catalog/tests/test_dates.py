import datetime as dt
import unittest

from libcat.util.dates import display_year, parse_date, years_of


class DatesTest(unittest.TestCase):
    def test_parse_partial_dates(self):
        self.assertEqual(parse_date("2019"), dt.date(2019, 1, 1))
        self.assertEqual(parse_date("2019-05"), dt.date(2019, 5, 1))
        self.assertEqual(parse_date("2019.05.17"), dt.date(2019, 5, 17))
        self.assertIsNone(parse_date("2019-13-01"))
        self.assertIsNone(parse_date("spring 2019"))

    def test_years_of(self):
        self.assertEqual(years_of("2019-05-01"), {2019})
        self.assertEqual(years_of("1998"), {1998})
        self.assertEqual(years_of("c. 1900"), set())
        self.assertEqual(years_of(None), set())
        self.assertEqual(years_of(""), set())

    def test_display_year(self):
        self.assertEqual(display_year("2021-01"), "2021")
        self.assertEqual(display_year(None), "n.d.")
