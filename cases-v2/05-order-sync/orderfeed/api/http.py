"""Request/response objects and parameter parsing."""
from dataclasses import dataclass, field

from orderfeed.models import InvalidRequest


@dataclass
class Request:
    method: str
    path: str
    params: dict = field(default_factory=dict)


@dataclass
class Response:
    status: int
    body: dict

    @classmethod
    def ok(cls, body):
        return cls(200, body)

    @classmethod
    def error(cls, status, code, message):
        return cls(status, {"error": code, "message": message})


def int_param(params, name, default=None, minimum=None, maximum=None):
    raw = params.get(name)
    if raw is None or raw == "":
        if default is None:
            raise InvalidRequest(f"{name} is required")
        return default
    try:
        value = int(raw)
    except (TypeError, ValueError):
        raise InvalidRequest(f"{name} must be an integer") from None
    if minimum is not None and value < minimum:
        raise InvalidRequest(f"{name} must be >= {minimum}")
    if maximum is not None and value > maximum:
        raise InvalidRequest(f"{name} must be <= {maximum}")
    return value


def str_param(params, name, default=None):
    raw = params.get(name)
    if raw is None or str(raw).strip() == "":
        if default is None:
            raise InvalidRequest(f"{name} is required")
        return default
    return str(raw).strip()
