import unittest

from romanizer import guide, romanize


class RomanizerHiddenTest(unittest.TestCase):
    def test_word_final_g(self):
        self.assertEqual(romanize("Greg"), "greg")
        self.assertEqual(romanize("Doug"), "doug")

    def test_word_final_sc(self):
        self.assertEqual(romanize("Bosc"), "bosk")
        self.assertEqual(romanize("Brusc"), "brusk")

    def test_soft_and_hard_still_work(self):
        self.assertEqual(romanize("Gemma"), "jemma")
        self.assertEqual(romanize("Gino"), "jino")
        self.assertEqual(romanize("Gabriele"), "gabriele")
        self.assertEqual(romanize("Cosimo"), "kosimo")
        self.assertEqual(romanize("Scala"), "skala")
        self.assertEqual(romanize("Ghita"), "gita")
        self.assertEqual(romanize("Luca"), "luka")

    def test_full_names(self):
        self.assertEqual(guide("Ludovic Gallo"), "Ludovik Gallo")
        self.assertEqual(guide("Eric Pisc"), "Erik Pisk")


if __name__ == "__main__":
    unittest.main()
