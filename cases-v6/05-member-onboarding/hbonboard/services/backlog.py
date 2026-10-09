"""Admin console: where every member stands in the onboarding funnel."""
from hbonboard.models import ACTIVE, PENDING, REJECTED, REVIEW
from hbonboard.util.phone import mask_phone


class BacklogReport:
    def __init__(self, repo, settings, cache):
        self.repo = repo
        self.settings = settings
        self.cache = cache

    def invalidate(self):
        self.cache.invalidate("backlog")

    def summary(self):
        cached = self.cache.get("backlog")
        if cached is not None:
            return cached
        members = sorted(self.repo.all(), key=lambda m: m.member_id)
        counts = {s: 0 for s in (PENDING, ACTIVE, REJECTED, REVIEW)}
        by_channel = {}
        for member in members:
            counts[member.status] = counts.get(member.status, 0) + 1
            if member.status == PENDING:
                by_channel[member.channel] = by_channel.get(member.channel, 0) + 1
        pending = [
            {
                "id": m.member_id,
                "phone": mask_phone(m.phone),
                "channel": m.channel,
                "kyc_state": m.kyc_state,
                "job": m.kyc_job,
            }
            for m in members if m.status == PENDING
        ]
        report = {
            "total": len(members),
            "counts": counts,
            "pending_by_channel": dict(sorted(by_channel.items())),
            "pending": pending[: self.settings.pending_preview],
        }
        self.cache.put("backlog", report)
        return report
