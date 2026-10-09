"""Month-end settlement: posts approved claims to the vendor ledger."""
from hbexpense.ledger.entries import build_entry
from hbexpense.models import APPROVED, POSTED


class SettlementService:
    def __init__(self, repo, ledger, settings, on_posted=None):
        self.repo = repo
        self.ledger = ledger
        self.settings = settings
        self.on_posted = on_posted

    def run(self, month):
        posted, failed = [], []
        claims = sorted(
            (c for c in self.repo.in_month(month) if c.status == APPROVED),
            key=lambda c: (c.spent_on, c.claim_id),
        )
        for claim in claims:
            entry = build_entry(claim, self.settings)
            try:
                receipt = self.ledger.post(entry)
            except Exception as exc:  # keep going; failures are reported
                failed.append({"id": claim.claim_id, "error": str(exc)})
                continue
            claim.receipt = receipt
            claim.status = POSTED
            claim.history.append(POSTED)
            posted.append({"id": claim.claim_id, "receipt": receipt})
        if posted and self.on_posted:
            self.on_posted(month)
        return {"month": month, "posted": posted, "failed": failed}
