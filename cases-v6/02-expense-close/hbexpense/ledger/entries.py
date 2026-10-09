"""Turns an approved expense claim into a ledger entry."""
from hbexpense.models import LedgerEntry
from hbexpense.util.text import clip_chars, clip_utf8, normalize


def compose_memo(claim):
    """Memo shown to the finance team: claim id, claimant, what it was for."""
    parts = [claim.claim_id, claim.claimant, claim.description]
    return normalize(" ".join(p for p in parts if p))


def build_entry(claim, settings):
    account = settings.accounts.get(claim.category)
    if account is None:
        raise ValueError(f"no ledger account for category {claim.category}")
    memo = clip_chars(compose_memo(claim), settings.ledger_memo_chars)
    memo = clip_utf8(memo, settings.ledger_memo_bytes)
    return LedgerEntry(
        account=account,
        amount=claim.amount,
        date=claim.spent_on,
        ref=claim.claim_id,
        memo=memo,
    )
