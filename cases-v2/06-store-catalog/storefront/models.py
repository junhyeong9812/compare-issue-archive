"""Domain types and errors."""
from dataclasses import asdict, dataclass


@dataclass(frozen=True)
class Product:
    sku: str
    name: str
    price: int
    stock: int = 0
    shared: bool = False

    def to_dict(self):
        return asdict(self)

    @classmethod
    def from_dict(cls, data):
        return cls(
            sku=data["sku"],
            name=data["name"],
            price=int(data["price"]),
            stock=int(data.get("stock", 0)),
            shared=bool(data.get("shared", False)),
        )


class NotFound(LookupError):
    pass


class Rejected(ValueError):
    """A revision failed validation and was rolled back."""

    def __init__(self, problems):
        super().__init__("; ".join(problems))
        self.problems = list(problems)


class BadRequest(ValueError):
    pass
