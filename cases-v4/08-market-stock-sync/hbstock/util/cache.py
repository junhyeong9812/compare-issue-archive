"""Tiny TTL cache for computed reports."""
import time


class TTLCache:
    def __init__(self, ttl, clock=None):
        self.ttl = ttl
        self._clock = clock or time.monotonic
        self._data = {}

    def get(self, key):
        item = self._data.get(key)
        if item is None:
            return None
        stored_at, value = item
        if self._clock() - stored_at > self.ttl:
            del self._data[key]
            return None
        return value

    def put(self, key, value):
        self._data[key] = (self._clock(), value)

    def invalidate(self, prefix=""):
        for key in [k for k in self._data if str(k).startswith(prefix)]:
            del self._data[key]

    def clear(self):
        self._data.clear()
