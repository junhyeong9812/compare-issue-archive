"""Identity-check run: sends new members to the vendor and applies verdicts."""
from hbonboard.kyc.requests import build_batch
from hbonboard.kyc.verdicts import apply_verdict
from hbonboard.models import ACTIVE, KYC_NEW, KYC_SENT, REVIEW
from hbonboard.util.dates import age_on


class VerificationService:
    def __init__(self, repo, client, settings, today, notifier=None, on_resolved=None):
        self.repo = repo
        self.client = client
        self.settings = settings
        self.today = today
        self.notifier = notifier
        self.on_resolved = on_resolved

    def _send_new(self):
        fresh = sorted(self.repo.by_kyc_state(KYC_NEW), key=lambda m: m.member_id)
        if not fresh:
            return [], 0
        by_id = {m.member_id: m for m in fresh}
        requests = build_batch(fresh)
        size = self.settings.idchk_batch_lines
        jobs = []
        for start in range(0, len(requests), size):
            chunk = requests[start:start + size]
            ticket = self.client.submit(chunk)
            jobs.append(ticket.job_id)
            for request in chunk:
                member = by_id[request.ref]
                member.kyc_state = KYC_SENT
                member.kyc_job = ticket.job_id
                member.history.append(KYC_SENT)
        return jobs, len(fresh)

    def _collect(self):
        resolved, waiting = [], []
        for member in sorted(self.repo.by_kyc_state(KYC_SENT), key=lambda m: m.member_id):
            code = self.client.result(member.member_id)
            if code is None:
                # the vendor has not answered for this one yet; ask again next run
                waiting.append(member.member_id)
                continue
            apply_verdict(member, code)
            self._age_gate(member)
            if self.notifier:
                self.notifier.verdict(member)
            resolved.append({"id": member.member_id, "status": member.status})
        return resolved, waiting

    def _age_gate(self, member):
        # second age check right before a member becomes active
        # (inspection follow-up 2025-05, item 4)
        # TODO: sign-up already refuses anyone under adult_age -- is this still needed?
        if member.status == ACTIVE and age_on(member.birth, self.today) < self.settings.adult_age:
            member.status = REVIEW
            member.history[-1] = REVIEW

    def run(self):
        jobs, sent = self._send_new()
        resolved, waiting = self._collect()
        if resolved and self.on_resolved:
            self.on_resolved()
        return {"jobs": jobs, "sent": sent, "resolved": resolved, "waiting": len(waiting)}
