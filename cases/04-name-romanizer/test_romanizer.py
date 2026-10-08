import unittest

from romanizer import guide, romanize


class RomanizerTest(unittest.TestCase):
    def test_common_names(self):
        self.assertEqual(romanize("Cesare"), "chesare")
        self.assertEqual(romanize("Carla"), "karla")
        self.assertEqual(romanize("Giulia"), "jiulia")
        self.assertEqual(romanize("Chiara"), "kiara")
        self.assertEqual(romanize("Scipione"), "shipione")

    def test_names_ending_in_c(self):
        self.assertEqual(romanize("Marc"), "mark")
        self.assertEqual(guide("Isaac Bianchi"), "Isaak Bianki")


if __name__ == "__main__":
    unittest.main()
