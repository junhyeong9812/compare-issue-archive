import datetime
import unittest

from hbonboard.util.dates import age_on, compact
from hbonboard.util.phone import display_phone, is_mobile, mask_phone, normalize_phone
from hbonboard.util.text import mask_name, normalize_name, vendor_name


class PhoneTest(unittest.TestCase):
    def test_normalize(self):
        self.assertEqual(normalize_phone("010-1234-5678"), "01012345678")
        self.assertEqual(normalize_phone("+82 10 1234 5678"), "01012345678")
        self.assertEqual(normalize_phone("(011)742.3306"), "0117423306")
        self.assertEqual(normalize_phone(None), "")

    def test_is_mobile(self):
        self.assertTrue(is_mobile("01012345678"))
        self.assertTrue(is_mobile("0167423306"))
        self.assertFalse(is_mobile("0212345678"))

    def test_mask_and_display(self):
        self.assertEqual(mask_phone("01012345678"), "010-****-5678")
        self.assertEqual(display_phone("0117423306"), "011-742-3306")


class TextAndDatesTest(unittest.TestCase):
    def test_normalize_name(self):
        self.assertEqual(normalize_name(" 남궁\t\t민수​ "), "남궁 민수")
        self.assertEqual(normalize_name(None), "")

    def test_mask_name(self):
        self.assertEqual(mask_name("홍길동"), "홍*동")
        self.assertEqual(mask_name("이도"), "이*")
        self.assertEqual(mask_name("Kim Minseo"), "K********o")

    def test_vendor_name_drops_spaces(self):
        self.assertEqual(vendor_name("황보 현"), "황보현")

    def test_age_and_compact(self):
        today = datetime.date(2026, 10, 9)
        self.assertEqual(age_on("2007-10-09", today), 19)
        self.assertEqual(age_on("2007-10-10", today), 18)
        self.assertEqual(compact("1999-05-21"), "19990521")


if __name__ == "__main__":
    unittest.main()
