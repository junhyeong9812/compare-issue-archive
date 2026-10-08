"""Domain objects and service-level errors."""
from dataclasses import dataclass, field


class NotFound(Exception):
    pass


class Conflict(Exception):
    pass


class LedgerError(Exception):
    """The vendor ledger CLI rejected a call (opaque vendor code in .code)."""

    def __init__(self, code, command):
        super().__init__(f"ledger {command} failed: {code}")
        self.code = code
        self.command = command


SUBMITTED = "submitted"
APPROVED = "approved"
REJECTED = "rejected"
POSTED = "posted"


@dataclass
class Claim:
    claim_id: str
    claimant: str
    cost_center: str
    category: str
    amount: int
    spent_on: str           # YYYY-MM-DD
    description: str
    status: str = SUBMITTED
    receipt: str = ""
    history: list = field(default_factory=list)

    def to_dict(self):
        return {
            "id": self.claim_id,
            "claimant": self.claimant,
            "cost_center": self.cost_center,
            "category": self.category,
            "amount": self.amount,
            "spent_on": self.spent_on,
            "description": self.description,
            "status": self.status,
            "receipt": self.receipt,
        }


@dataclass(frozen=True)
class LedgerEntry:
    account: str
    amount: int
    date: str
    ref: str
    memo: str


@dataclass(frozen=True)
class LedgerRow:
    receipt: str
    date: str
    account: str
    amount: int
    ref: str
    memo: str
