"""Domain types and errors."""
from dataclasses import dataclass


@dataclass(frozen=True)
class Order:
    id: int
    customer: str
    amount: int          # minor units
    created_at: int      # unix seconds
    channel: str = "web"
    cancelled: bool = False

    def to_dict(self):
        return {
            "id": self.id,
            "customer": self.customer,
            "amount": self.amount,
            "created_at": self.created_at,
            "channel": self.channel,
        }


class NotFound(LookupError):
    pass


class InvalidRequest(ValueError):
    pass


class StaleCursor(RuntimeError):
    """The listing changed in a way the page token cannot follow."""
