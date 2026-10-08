"""Domain objects."""
from dataclasses import dataclass, field


@dataclass
class Record:
    id: str
    title: str
    creators: list = field(default_factory=list)
    date: object = None          # issued date(s) exactly as delivered by the feed
    subjects: list = field(default_factory=list)
    language: str = "und"
    publisher: str = ""

    def to_dict(self):
        return {
            "id": self.id,
            "title": self.title,
            "creators": list(self.creators),
            "date": self.date,
            "subjects": list(self.subjects),
            "language": self.language,
            "publisher": self.publisher,
        }


@dataclass
class Loan:
    record_id: str
    member_id: str
    due_day: int
    returned: bool = False


class NotFound(LookupError):
    pass


class Conflict(RuntimeError):
    pass
