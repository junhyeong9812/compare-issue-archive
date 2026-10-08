import unittest

from libcat.util.feed_format import as_list, first, parse_feed


class FeedFormatTest(unittest.TestCase):
    def test_records_split_on_blank_lines(self):
        recs = parse_feed("id: a\ntitle: A\n\n\nid: b\ntitle: B\n")
        self.assertEqual([r["id"] for r in recs], ["a", "b"])

    def test_repeated_keys(self):
        rec = parse_feed("id: a\nsubject: x\nsubject: y\nsubject: z\n")[0]
        self.assertEqual(rec["subject"], ["x", "y", "z"])
        self.assertEqual(as_list(rec["subject"]), ["x", "y", "z"])
        self.assertEqual(as_list(rec["id"]), ["a"])

    def test_comments_and_colons_in_values(self):
        rec = parse_feed("# header\nid: a\ntitle: Salt: A Story\n")[0]
        self.assertEqual(rec["title"], "Salt: A Story")

    def test_malformed_line(self):
        with self.assertRaises(ValueError):
            parse_feed("id: a\nnot a field\n")

    def test_first(self):
        self.assertEqual(first(["x", "y"]), "x")
        self.assertEqual(first("x"), "x")
        self.assertIsNone(first(None))
