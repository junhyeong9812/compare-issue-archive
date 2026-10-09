"""Month-end expense report, read back from the ledger."""
from hbexpense.util.dates import month_bounds
from hbexpense.util.money import format_krw, total


class ReportService:
    def __init__(self, repo, ledger, cache):
        self.repo = repo
        self.ledger = ledger
        self.cache = cache

    def invalidate(self, month):
        self.cache.invalidate(f"monthly:{month}")

    def _rows(self, month):
        date_from, date_to = month_bounds(month)
        rows = self.ledger.list_rows(date_from, date_to)
        # a claim re-posted after a correction keeps the newest receipt only
        latest = {}
        for row in rows:
            seen = latest.get(row.ref)
            if seen is None or row.receipt > seen.receipt:
                latest[row.ref] = row
        return sorted(latest.values(), key=lambda r: (r.date, r.receipt))

    def monthly(self, month):
        key = f"monthly:{month}"
        cached = self.cache.get(key)
        if cached is not None:
            return cached
        rows = self._rows(month)
        by_account, by_cost_center, lines = {}, {}, []
        for row in rows:
            by_account[row.account] = by_account.get(row.account, 0) + row.amount
            claim = self.repo.by_ref(row.ref)
            center = claim.cost_center if claim else "UNKNOWN"
            by_cost_center[center] = by_cost_center.get(center, 0) + row.amount
            lines.append({
                "receipt": row.receipt,
                "date": row.date,
                "ref": row.ref,
                "account": row.account,
                "amount": row.amount,
                "memo": row.memo,
            })
        grand = total(r.amount for r in rows)
        report = {
            "month": month,
            "entries": len(rows),
            "total": grand,
            "total_display": format_krw(grand),
            "by_account": dict(sorted(by_account.items())),
            "by_cost_center": dict(sorted(by_cost_center.items())),
            "lines": lines,
        }
        self.cache.put(key, report)
        return report
