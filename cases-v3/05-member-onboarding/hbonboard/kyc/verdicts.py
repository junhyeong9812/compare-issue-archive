"""Vendor verdict codes -> member status."""
from hbonboard.models import ACTIVE, KYC_DONE, REJECTED, REVIEW

# 1 = identity matched, 2 = identity did not match; anything else goes to a person
VERDICT_STATUS = {1: ACTIVE, 2: REJECTED}


def status_for(code):
    return VERDICT_STATUS.get(code, REVIEW)


def apply_verdict(member, code):
    member.verdict = code
    member.status = status_for(code)
    member.kyc_state = KYC_DONE
    member.history.append(member.status)
    return member
