import unittest

from libcat.util.cache import TTLCache
from libcat.util.text import clean_title, fold, tokens


class TextTest(unittest.TestCase):
    def test_fold(self):
        self.assertEqual(fold("The Café"), "cafe")
        self.assertEqual(fold("An Atlas"), "atlas")

    def test_tokens(self):
        self.assertEqual(tokens("Salt and Iron: A History"), {"salt", "and", "iron", "a", "history"})

    def test_clean_title(self):
        self.assertEqual(clean_title("Field  Guide /"), "Field Guide")


class TTLCacheTest(unittest.TestCase):
    def test_expiry_with_fake_clock(self):
        now = [100.0]
        cache = TTLCache(10, clock=lambda: now[0])
        calls = []
        compute = lambda: calls.append(1) or len(calls)
        self.assertEqual(cache.get_or_compute("k", compute), 1)
        now[0] = 105.0
        self.assertEqual(cache.get_or_compute("k", compute), 1)
        now[0] = 111.0
        self.assertEqual(cache.get_or_compute("k", compute), 2)
        cache.clear()
        self.assertEqual(cache.get_or_compute("k", compute), 3)
