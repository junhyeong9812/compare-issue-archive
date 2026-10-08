"""Tiny TTL cache for dashboard aggregates."""
import time


class TTLCache:
    def __init__(self, ttl, clock=None):
        self.ttl = ttl
        self._clock = clock or time.monotonic
        self._entries = {}

    def get_or_compute(self, key, compute):
        now = self._clock()
        hit = self._entries.get(key)
        if hit is not None and now - hit[0] < self.ttl:
            return hit[1]
        value = compute()
        self._entries[key] = (now, value)
        return value

    def clear(self):
        self._entries.clear()
