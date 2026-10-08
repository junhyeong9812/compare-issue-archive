"""Secondary indexes. Each index maps a key to the set of record ids."""
from collections import defaultdict

from libcat.util.dates import years_of


class _SetIndex:
    def __init__(self):
        self._buckets = defaultdict(set)

    def _keys(self, record):
        raise NotImplementedError

    def add(self, record):
        for key in self._keys(record):
            self._buckets[key].add(record.id)

    def remove(self, record):
        for key in self._keys(record):
            bucket = self._buckets.get(key)
            if bucket is not None:
                bucket.discard(record.id)
                if not bucket:
                    del self._buckets[key]

    def ids_for(self, key):
        return set(self._buckets.get(key, ()))

    def counts(self):
        return {k: len(v) for k, v in self._buckets.items()}

    def buckets(self):
        return {k: set(v) for k, v in self._buckets.items()}


class YearIndex(_SetIndex):
    def _keys(self, record):
        return years_of(record.date)

    def ids_between(self, year_from=None, year_to=None):
        out = set()
        for year, ids in self._buckets.items():
            if year_from is not None and year < year_from:
                continue
            if year_to is not None and year > year_to:
                continue
            out |= ids
        return out


class SubjectIndex(_SetIndex):
    def _keys(self, record):
        return set(record.subjects)
