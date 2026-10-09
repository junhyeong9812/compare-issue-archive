"""In-memory claim store (the real service keeps this in the portal DB)."""
from hbexpense.models import Conflict, NotFound


class ClaimRepository:
    def __init__(self):
        self._claims = {}
        self._seq = 0

    def next_id(self, month):
        self._seq += 1
        return f"EX-{month.replace('-', '')[2:]}{self._seq:03d}"

    def add(self, claim):
        if claim.claim_id in self._claims:
            raise Conflict(f"duplicate claim {claim.claim_id}")
        self._claims[claim.claim_id] = claim
        return claim

    def get(self, claim_id):
        try:
            return self._claims[claim_id]
        except KeyError:
            raise NotFound(f"claim {claim_id} not found") from None

    def by_status(self, status):
        return [c for c in self._claims.values() if c.status == status]

    def in_month(self, month):
        return [c for c in self._claims.values() if c.spent_on.startswith(month + "-")]

    def by_ref(self, ref):
        return self._claims.get(ref)

    def all(self):
        return list(self._claims.values())
