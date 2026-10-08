"""Clocks. Services take a clock so tests can pin time."""
import time


class SystemClock:
    def now(self):
        return int(time.time())


class FakeClock:
    def __init__(self, start=1_760_000_000):
        self._now = start

    def now(self):
        return self._now

    def advance(self, seconds):
        self._now += seconds
        return self._now
