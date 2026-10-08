"""Request/response value objects and query-string helpers."""
from dataclasses import dataclass, field


@dataclass
class Request:
    method: str
    path: str
    query: dict = field(default_factory=dict)
    body: str = ""


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


def int_param(query, name, default=None, minimum=None, maximum=None):
    raw = query.get(name)
    if raw is None or raw == "":
        return default
    try:
        value = int(str(raw).strip())
    except ValueError:
        raise ValueError(f"{name} must be an integer") from None
    if minimum is not None and value < minimum:
        raise ValueError(f"{name} must be >= {minimum}")
    if maximum is not None and value > maximum:
        raise ValueError(f"{name} must be <= {maximum}")
    return value


def str_param(query, name, default=None):
    raw = query.get(name)
    if raw is None:
        return default
    raw = str(raw).strip()
    return raw or default
