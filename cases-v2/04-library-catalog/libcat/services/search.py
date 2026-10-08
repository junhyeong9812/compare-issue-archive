"""Catalog search: free text, subject and publication-year filters."""
from libcat.models import NotFound
from libcat.util.dates import display_year
from libcat.util.feed_format import first
from libcat.util.text import fold, tokens


class SearchService:
    def __init__(self, repo, settings):
        self.repo = repo
        self.settings = settings

    def get(self, record_id):
        record = self.repo.get(record_id)
        if record is None:
            raise NotFound(f"no record {record_id}")
        return record

    def search(self, text=None, subject=None, year_from=None, year_to=None, page=1, page_size=20):
        if year_from is not None and year_to is not None and year_from > year_to:
            raise ValueError("from must not be after to")
        candidates = None
        if year_from is not None or year_to is not None:
            candidates = self.repo.years.ids_between(year_from, year_to)
        if subject:
            by_subject = self.repo.subjects.ids_for(subject.lower())
            candidates = by_subject if candidates is None else candidates & by_subject
        if candidates is None:
            candidates = set(self.repo.all_ids())

        records = [self.repo.get(i) for i in candidates]
        if text:
            wanted = tokens(text)
            records = [r for r in records if wanted <= self._haystack(r)]

        records.sort(key=lambda r: (fold(r.title), r.id))
        total = len(records)
        start = (page - 1) * page_size
        items = [
            {"id": r.id, "title": r.title, "creators": r.creators, "year": display_year(first(r.date))}
            for r in records[start:start + page_size]
        ]
        return {"total": total, "page": page, "items": items}

    @staticmethod
    def _haystack(record):
        words = tokens(record.title)
        for creator in record.creators:
            words |= tokens(creator)
        return words
