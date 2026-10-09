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


def int_param(query, name, default):
    raw = str_param(query, name)
    if raw is None:
        return default
    try:
        value = int(raw)
    except ValueError:
        raise ValueError(f"{name} must be an integer") from None
    if value < 0:
        raise ValueError(f"{name} must not be negative")
    return value
