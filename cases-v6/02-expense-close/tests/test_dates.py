import unittest

from hbexpense.util.dates import month_bounds, month_of, previous_month


class DatesTest(unittest.TestCase):
    def test_month_bounds(self):
        self.assertEqual(month_bounds("2026-09"), ("2026-09-01", "2026-09-30"))
        self.assertEqual(month_bounds("2028-02"), ("2028-02-01", "2028-02-29"))
        self.assertEqual(month_bounds("2026-12"), ("2026-12-01", "2026-12-31"))

    def test_month_of(self):
        self.assertEqual(month_of("2026-09-30"), "2026-09")
        with self.assertRaises(ValueError):
            month_of("2026-09-31")

    def test_previous_month(self):
        self.assertEqual(previous_month("2026-01"), "2025-12")
        self.assertEqual(previous_month("2026-10"), "2026-09")


if __name__ == "__main__":
    unittest.main()
