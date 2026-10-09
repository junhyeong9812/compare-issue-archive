"""Request/response value objects and query-string helpers."""
import re
from dataclasses import dataclass, field

_MONTH = re.compile(r"^(\d{4})-(\d{2})$")


@dataclass
class Request:
    method: str
    path: str
    query: dict = field(default_factory=dict)
    body: object = None


@dataclass
class Response:
    status: int
    body: dict

    @classmethod
    def ok(cls, body):
        return cls(200, body)

    @classmethod
    def error(cls, status, message):
        return cls(status, {"error": message})


def month_param(query, name="month"):
    raw = str(query.get(name, "")).strip()
    match = _MONTH.match(raw)
    if not match or not 1 <= int(match.group(2)) <= 12:
        raise ValueError(f"{name} must look like YYYY-MM")
    return raw


def str_param(query, name, default=None):
    raw = query.get(name)
    if raw is None:
        return default
    raw = str(raw).strip()
    return raw or default
