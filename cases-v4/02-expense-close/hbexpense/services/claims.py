"""Claim submission and approval workflow."""
import re

from hbexpense.models import APPROVED, REJECTED, SUBMITTED, Claim, Conflict
from hbexpense.util.dates import month_of, parse_date
from hbexpense.util.text import normalize

_COST_CENTER = re.compile(r"^CC\d{3}$")


class ClaimService:
    def __init__(self, repo, settings):
        self.repo = repo
        self.settings = settings

    def validate(self, payload):
        claimant = normalize(payload.get("claimant"))
        description = normalize(payload.get("description"))
        cost_center = str(payload.get("cost_center", "")).strip().upper()
        category = str(payload.get("category", "")).strip().upper()
        if not claimant or len(claimant) > self.settings.claimant_max_chars:
            raise ValueError("claimant is required (max %d characters)" % self.settings.claimant_max_chars)
        if not description:
            raise ValueError("description is required")
        if len(description) > self.settings.description_max_chars:
            raise ValueError("description is limited to %d characters" % self.settings.description_max_chars)
        if not _COST_CENTER.match(cost_center):
            raise ValueError("cost_center must look like CC123")
        if category not in self.settings.accounts:
            raise ValueError(f"unknown category {category}")
        try:
            amount = int(payload.get("amount"))
        except (TypeError, ValueError):
            raise ValueError("amount must be an integer") from None
        if not 0 < amount <= self.settings.max_amount:
            raise ValueError("amount out of range")
        spent_on = parse_date(payload.get("spent_on")).isoformat()
        return {
            "claimant": claimant,
            "description": description,
            "cost_center": cost_center,
            "category": category,
            "amount": amount,
            "spent_on": spent_on,
        }

    def submit(self, payload):
        fields = self.validate(payload)
        claim_id = self.repo.next_id(month_of(fields["spent_on"]))
        claim = Claim(claim_id=claim_id, **fields)
        claim.history.append(SUBMITTED)
        return self.repo.add(claim)

    def approve(self, claim_id):
        claim = self.repo.get(claim_id)
        if claim.status != SUBMITTED:
            raise Conflict(f"claim {claim_id} is {claim.status}")
        claim.status = APPROVED
        claim.history.append(APPROVED)
        return claim

    def reject(self, claim_id):
        claim = self.repo.get(claim_id)
        if claim.status != SUBMITTED:
            raise Conflict(f"claim {claim_id} is {claim.status}")
        claim.status = REJECTED
        claim.history.append(REJECTED)
        return claim
