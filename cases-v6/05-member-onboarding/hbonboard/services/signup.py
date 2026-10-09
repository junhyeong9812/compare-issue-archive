"""Sign-up form validation and registration."""
from hbonboard.models import Conflict, Member, PENDING
from hbonboard.util.dates import age_on, parse_date
from hbonboard.util.phone import is_mobile, normalize_phone
from hbonboard.util.text import normalize_name


class SignupService:
    def __init__(self, repo, settings, today):
        self.repo = repo
        self.settings = settings
        self.today = today

    def validate(self, payload):
        name = normalize_name(payload.get("name"))
        if not name or len(name) > self.settings.name_max_chars:
            raise ValueError("name is required (max %d characters)" % self.settings.name_max_chars)
        birth = parse_date(payload.get("birth"))
        if birth > self.today:
            raise ValueError("birth date is in the future")
        if age_on(birth.isoformat(), self.today) < self.settings.adult_age:
            raise ValueError("members must be %d or older" % self.settings.adult_age)
        phone = normalize_phone(payload.get("phone"))
        if not is_mobile(phone):
            raise ValueError("phone must be a mobile number")
        channel = str(payload.get("channel", "app")).strip().lower()
        if channel not in self.settings.channels:
            raise ValueError(f"unknown channel {channel}")
        return {"name": name, "birth": birth.isoformat(), "phone": phone, "channel": channel}

    def register(self, payload):
        fields = self.validate(payload)
        if self.repo.by_phone(fields["phone"]) is not None:
            raise Conflict("phone number already registered")
        member = Member(member_id=self.repo.next_id(), **fields)
        member.history.append(PENDING)
        return self.repo.add(member)
