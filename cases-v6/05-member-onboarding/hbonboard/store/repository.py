"""In-memory member store (the real service keeps this in the member DB)."""
from hbonboard.models import Conflict, NotFound


class MemberRepository:
    def __init__(self, id_prefix="M2610"):
        self._members = {}
        self._by_phone = {}
        self._seq = 0
        self._prefix = id_prefix

    def next_id(self):
        self._seq += 1
        return f"{self._prefix}{self._seq:03d}"

    def add(self, member):
        if member.member_id in self._members:
            raise Conflict(f"duplicate member {member.member_id}")
        if member.phone in self._by_phone:
            raise Conflict("phone number already registered")
        self._members[member.member_id] = member
        self._by_phone[member.phone] = member.member_id
        return member

    def get(self, member_id):
        try:
            return self._members[member_id]
        except KeyError:
            raise NotFound(f"member {member_id} not found") from None

    def by_phone(self, phone):
        member_id = self._by_phone.get(phone)
        return self._members.get(member_id) if member_id else None

    def by_status(self, status):
        return [m for m in self._members.values() if m.status == status]

    def by_kyc_state(self, state):
        return [m for m in self._members.values() if m.kyc_state == state]

    def all(self):
        return list(self._members.values())
