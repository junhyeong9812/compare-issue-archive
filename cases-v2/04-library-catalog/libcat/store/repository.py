"""In-memory record store with secondary indexes kept in sync on save."""
from libcat.store.index import SubjectIndex, YearIndex


class Repository:
    def __init__(self):
        self._records = {}
        self.years = YearIndex()
        self.subjects = SubjectIndex()

    def exists(self, record_id):
        return record_id in self._records

    def get(self, record_id):
        return self._records.get(record_id)

    def all_ids(self):
        return list(self._records)

    def count(self):
        return len(self._records)

    def save(self, record):
        previous = self._records.get(record.id)
        if previous is not None:
            self.years.remove(previous)
            self.subjects.remove(previous)
        self._records[record.id] = record
        self.years.add(record)
        self.subjects.add(record)
