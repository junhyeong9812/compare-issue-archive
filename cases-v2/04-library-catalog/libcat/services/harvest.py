"""Publisher feed ingestion."""
from libcat.models import Record
from libcat.util.feed_format import as_list, first, parse_feed
from libcat.util.text import clean_title


class HarvestService:
    REQUIRED = ("id", "title")

    def __init__(self, repo, on_change=None):
        self.repo = repo
        self.on_change = on_change or (lambda: None)

    def ingest(self, text):
        added, updated, skipped = 0, 0, []
        for fields in parse_feed(text):
            missing = [k for k in self.REQUIRED if not fields.get(k)]
            if missing:
                skipped.append({"id": first(fields.get("id")), "missing": missing})
                continue
            record = self._to_record(fields)
            if self.repo.exists(record.id):
                updated += 1
            else:
                added += 1
            self.repo.save(record)
        if added or updated:
            self.on_change()
        return {"added": added, "updated": updated, "skipped": skipped}

    @staticmethod
    def _to_record(fields):
        return Record(
            id=first(fields["id"]),
            title=clean_title(first(fields["title"])),
            creators=as_list(fields.get("creator")),
            date=fields.get("date"),
            subjects=[s.lower() for s in as_list(fields.get("subject"))],
            language=first(fields.get("language")) or "und",
            publisher=first(fields.get("publisher")) or "",
        )
