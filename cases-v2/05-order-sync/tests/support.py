"""Shared fixtures for API-level tests."""
from orderfeed.app import build_app
from orderfeed.util.clock import FakeClock


class Shop:
    """An app plus an independent record of which orders should be active."""

    def __init__(self, orders=12, step=60):
        self.clock = FakeClock()
        self.app = build_app(clock=self.clock)
        self.expected = {}
        for i in range(orders):
            self.clock.advance(step)
            self.place(f"customer-{i}", 1000 + i)

    def place(self, customer, amount, created_at=None, channel="web"):
        params = {"customer": customer, "amount": amount, "channel": channel}
        if created_at is not None:
            params["created_at"] = created_at
        resp = self.app.post("/orders", params)
        assert resp.status == 200, resp.body
        self.expected[resp.body["id"]] = resp.body
        return resp.body

    def cancel(self, order_id):
        resp = self.app.post(f"/orders/{order_id}/cancel")
        assert resp.status == 200, resp.body
        del self.expected[order_id]

    def newest_first(self):
        return sorted(self.expected, key=lambda i: (self.expected[i]["created_at"], i), reverse=True)


class InterferingTransport:
    """Calls the API like the real client, and runs `action` once right after the n-th listing call."""

    def __init__(self, app, after_call=1, action=None):
        self.app = app
        self.after_call = after_call
        self.action = action
        self.calls = 0

    def __call__(self, path, params):
        resp = self.app.get(path, params)
        self.calls += 1
        if self.action is not None and self.calls == self.after_call:
            action, self.action = self.action, None
            action()
        return resp
