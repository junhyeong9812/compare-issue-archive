"""Domain objects and service-level errors."""
from dataclasses import dataclass, field


class NotFound(Exception):
    pass


class Conflict(Exception):
    pass


class VendorError(Exception):
    """The identity-verification CLI rejected a call (opaque vendor code in .code)."""

    def __init__(self, code, command):
        super().__init__(f"idchk {command} failed: {code}")
        self.code = code
        self.command = command


# member status, as shown in the app and the admin console
PENDING = "pending"        # 확인 대기
ACTIVE = "active"
REJECTED = "rejected"
REVIEW = "review"          # handed to the compliance desk

# where the identity check stands
KYC_NEW = "new"
KYC_SENT = "sent"
KYC_DONE = "done"


@dataclass
class Member:
    member_id: str
    name: str
    birth: str              # YYYY-MM-DD
    phone: str              # digits only, 010xxxxxxxx
    channel: str
    status: str = PENDING
    kyc_state: str = KYC_NEW
    kyc_job: str = ""
    verdict: int = 0
    history: list = field(default_factory=list)

    def to_dict(self, mask=None):
        phone = mask(self.phone) if mask else self.phone
        return {
            "id": self.member_id,
            "name": self.name,
            "birth": self.birth,
            "phone": phone,
            "channel": self.channel,
            "status": self.status,
            "kyc": {"state": self.kyc_state, "job": self.kyc_job},
        }


@dataclass(frozen=True)
class KycRequest:
    ref: str
    name: str
    birth: str              # YYYYMMDD, as the vendor wants it
    phone: str


@dataclass(frozen=True)
class JobTicket:
    job_id: str
    lines: int
