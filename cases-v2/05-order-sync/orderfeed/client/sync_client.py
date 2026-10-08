"""Accounting sync client: pulls every active order through the paged API."""


class SyncError(RuntimeError):
    pass


class _Restart(Exception):
    pass


class FeedClient:
    def __init__(self, transport, page_size=50, max_restarts=5):
        """transport(path, params) -> object with .status and .body"""
        self.transport = transport
        self.page_size = page_size
        self.max_restarts = max_restarts
        self.restarts = 0

    def fetch_all(self):
        self.restarts = 0
        while True:
            try:
                return self._walk()
            except _Restart:
                self.restarts += 1
                if self.restarts > self.max_restarts:
                    raise SyncError("the order listing kept changing during sync") from None

    def _walk(self):
        seen, out, cursor = set(), [], None
        while True:
            params = {"limit": self.page_size}
            if cursor:
                params["cursor"] = cursor
            resp = self.transport("/orders", params)
            if resp.status == 409:
                raise _Restart()
            if resp.status != 200:
                raise SyncError(f"listing failed: {resp.status} {resp.body}")
            for item in resp.body["items"]:
                if item["id"] in seen:
                    continue
                seen.add(item["id"])
                out.append(item)
            cursor = resp.body.get("next_cursor")
            if not cursor:
                return out


def total_amount(orders):
    return sum(o["amount"] for o in orders)
