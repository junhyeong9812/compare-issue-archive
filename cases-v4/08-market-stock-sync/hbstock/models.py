"""Domain objects and service-level errors."""
from dataclasses import dataclass, field


class NotFound(Exception):
    pass


class Conflict(Exception):
    pass


class VendorError(Exception):
    """The stock sync CLI rejected a call (opaque vendor code in .code)."""

    def __init__(self, code, command):
        super().__init__(f"stockpush {command} failed: {code}")
        self.code = code
        self.command = command


@dataclass
class Product:
    sku: str
    name: str
    category: str
    price: int
    on_hand: int = 0
    reserved: int = 0

    @property
    def available(self):
        return self.on_hand - self.reserved

    def to_dict(self):
        return {
            "sku": self.sku,
            "name": self.name,
            "category": self.category,
            "price": self.price,
            "on_hand": self.on_hand,
            "reserved": self.reserved,
            "available": self.available,
        }


OPEN = "open"
SHIPPED = "shipped"
CANCELLED = "cancelled"


@dataclass
class Order:
    order_id: str
    sku: str
    qty: int
    status: str = OPEN
    history: list = field(default_factory=list)

    def to_dict(self):
        return {"id": self.order_id, "sku": self.sku, "qty": self.qty, "status": self.status}


@dataclass(frozen=True)
class MarketRow:
    sku: str
    qty: int
    st: int
    transfer: str


@dataclass(frozen=True)
class Transfer:
    transfer_id: str
    lines: int
