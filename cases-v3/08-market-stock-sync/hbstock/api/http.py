"""Request/response value objects and query-string helpers."""
from dataclasses import dataclass, field


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


def str_param(query, name, default=None):
    raw = query.get(name)
    if raw is None:
        return default
    raw = str(raw).strip()
    return raw or default


def bool_param(query, name):
    return str_param(query, name, "").lower() in ("1", "true", "yes")
