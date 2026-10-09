"""Identity-check run: sends new members to the vendor and applies verdicts."""
from hbonboard.kyc.requests import build_batch
from hbonboard.kyc.verdicts import apply_verdict
from hbonboard.models import KYC_NEW, KYC_SENT


class VerificationService:
    def __init__(self, repo, client, settings, notifier=None, on_resolved=None):
        self.repo = repo
        self.client = client
        self.settings = settings
        self.notifier = notifier
        self.on_resolved = on_resolved

    def _send_new(self):
        fresh = sorted(self.repo.by_kyc_state(KYC_NEW), key=lambda m: m.member_id)
        if not fresh:
            return [], 0
        ticket = self.client.submit(build_batch(fresh))
        for member in fresh:
            member.kyc_state = KYC_SENT
            member.kyc_job = ticket.job_id
            member.history.append(KYC_SENT)
        return [ticket.job_id], len(fresh)

    def _collect(self):
        resolved, waiting = [], []
        for member in sorted(self.repo.by_kyc_state(KYC_SENT), key=lambda m: m.member_id):
            code = self.client.result(member.member_id)
            if code is None:
                # the vendor has not answered for this one yet; ask again next run
                waiting.append(member.member_id)
                continue
            apply_verdict(member, code)
            if self.notifier:
                self.notifier.verdict(member)
            resolved.append({"id": member.member_id, "status": member.status})
        return resolved, waiting

    def run(self):
        jobs, sent = self._send_new()
        resolved, waiting = self._collect()
        if resolved and self.on_resolved:
            self.on_resolved()
        return {"jobs": jobs, "sent": sent, "resolved": resolved, "waiting": len(waiting)}
