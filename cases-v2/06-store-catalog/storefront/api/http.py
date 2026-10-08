"""Request/response value objects."""
from dataclasses import dataclass
from typing import Any


@dataclass
class Request:
    method: str
    path: str
    body: Any = None


@dataclass
class Response:
    status: int
    body: Any

    @classmethod
    def ok(cls, body):
        return cls(200, body)

    @classmethod
    def error(cls, status, message):
        return cls(status, {"error": message})
