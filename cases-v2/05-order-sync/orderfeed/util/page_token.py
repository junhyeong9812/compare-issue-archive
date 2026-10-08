"""Opaque page tokens for list endpoints.

A token records where the next page starts and which order was at the top of
the listing when the client began, so a walk that no longer matches the
listing is refused and the client starts over.
"""
import base64
import binascii
import json
from dataclasses import dataclass

from orderfeed.models import InvalidRequest, StaleCursor


@dataclass(frozen=True)
class PageToken:
    offset: int
    top: int


def encode(token):
    raw = json.dumps({"o": token.offset, "t": token.top}, separators=(",", ":")).encode()
    return base64.urlsafe_b64encode(raw).decode().rstrip("=")


def decode(text):
    padded = text + "=" * (-len(text) % 4)
    try:
        data = json.loads(base64.urlsafe_b64decode(padded.encode()))
        token = PageToken(int(data["o"]), int(data["t"]))
    except (binascii.Error, ValueError, KeyError, TypeError):
        raise InvalidRequest("malformed cursor") from None
    if token.offset < 0:
        raise InvalidRequest("malformed cursor")
    return token


def ensure_current(token, head_id):
    if head_id != token.top:
        raise StaleCursor("the listing changed since the first page; start again")
