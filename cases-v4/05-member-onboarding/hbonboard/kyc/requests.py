"""Turns members waiting for an identity check into vendor request lines."""
from hbonboard.models import KycRequest
from hbonboard.util.dates import compact
from hbonboard.util.text import vendor_name


def to_request(member):
    return KycRequest(
        ref=member.member_id,
        name=vendor_name(member.name),
        birth=compact(member.birth),
        phone=member.phone,
    )


def build_batch(members):
    """One line per member, in phone-number order (the vendor's matching key)."""
    seen, requests = set(), []
    for member in members:
        if member.member_id in seen:
            continue
        seen.add(member.member_id)
        requests.append(to_request(member))
    return sorted(requests, key=lambda r: r.phone)
