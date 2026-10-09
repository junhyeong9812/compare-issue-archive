"""Member notifications (the real service hands these to the SMS gateway)."""
from hbonboard.models import ACTIVE, REJECTED, REVIEW
from hbonboard.util.phone import mask_phone
from hbonboard.util.text import mask_name

TEMPLATES = {
    ACTIVE: "[한빛페이] {name}님, 본인확인이 완료되어 가입이 끝났습니다.",
    REJECTED: "[한빛페이] {name}님, 입력하신 정보로 본인확인이 되지 않았습니다. 앱에서 다시 시도해 주세요.",
    REVIEW: "[한빛페이] {name}님, 본인확인 결과를 담당자가 확인하고 있습니다.",
}


class Outbox:
    def __init__(self):
        self._messages = []

    def verdict(self, member):
        template = TEMPLATES.get(member.status)
        if template is None:
            return None
        message = {
            "member": member.member_id,
            "to": mask_phone(member.phone),
            "kind": member.status,
            "text": template.format(name=mask_name(member.name)),
        }
        self._messages.append(message)
        return message

    def messages(self, member_id=None):
        if member_id is None:
            return list(self._messages)
        return [m for m in self._messages if m["member"] == member_id]
