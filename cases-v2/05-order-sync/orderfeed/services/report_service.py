"""Daily sales report."""
from collections import OrderedDict

from orderfeed.util.money import format_amount

DAY = 86_400


class ReportService:
    def __init__(self, store, settings):
        self.store = store
        self.settings = settings

    def _local_day(self, ts):
        return (ts + self.settings.report_timezone_offset_hours * 3600) // DAY

    def daily(self, start=0, end=2**40):
        totals = OrderedDict()
        for order in reversed(self.store.active_between(start, end)):
            day = self._local_day(order.created_at)
            count, amount = totals.get(day, (0, 0))
            totals[day] = (count + 1, amount + order.amount)
        rows = []
        for day, (count, amount) in totals.items():
            rows.append({
                "day": day,
                "orders": count,
                "amount": amount,
                "display": format_amount(amount, self.settings.currency),
            })
        return {"days": rows}
