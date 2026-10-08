"""Loan desk: checkout and return with due-day bookkeeping."""
from libcat.models import Conflict, Loan, NotFound


class LoanService:
    def __init__(self, repo, settings):
        self.repo = repo
        self.settings = settings
        self._active = {}

    def checkout(self, record_id, member_id, day):
        if not record_id or not member_id:
            raise ValueError("record and member are required")
        if not self.repo.exists(record_id):
            raise NotFound(f"no record {record_id}")
        if record_id in self._active:
            raise Conflict(f"{record_id} is already on loan")
        held = sum(1 for loan in self._active.values() if loan.member_id == member_id)
        if held >= self.settings.max_loans_per_member:
            raise Conflict(f"{member_id} reached the loan limit")
        loan = Loan(record_id, member_id, day + self.settings.loan_days)
        self._active[record_id] = loan
        return loan

    def give_back(self, record_id, day):
        loan = self._active.pop(record_id, None)
        if loan is None:
            raise NotFound(f"{record_id} is not on loan")
        loan.returned = True
        return max(0, day - loan.due_day)
