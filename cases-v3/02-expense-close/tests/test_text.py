import unittest

from hbexpense.util.text import clip_chars, display_width, normalize, pad_display


class TextTest(unittest.TestCase):
    def test_normalize_collapses_whitespace_and_controls(self):
        self.assertEqual(normalize(" a\t\tb　c\x07 "), "a b c")
        self.assertEqual(normalize(None), "")

    def test_normalize_composes_hangul(self):
        decomposed = "한"  # 한 as jamo
        self.assertEqual(normalize(decomposed), "한")

    def test_clip_chars_counts_characters(self):
        self.assertEqual(clip_chars("가나다라마", 3), "가나다")
        self.assertEqual(clip_chars("abc", 5), "abc")
        self.assertEqual(clip_chars("ab cd", 3), "ab")
        self.assertEqual(clip_chars("abc", 0), "")

    def test_display_width(self):
        self.assertEqual(display_width("ab"), 2)
        self.assertEqual(display_width("가나"), 4)

    def test_pad_display(self):
        self.assertEqual(pad_display("가나다", 5), "가나 ")
        self.assertEqual(pad_display("ab", 4), "ab  ")


if __name__ == "__main__":
    unittest.main()
