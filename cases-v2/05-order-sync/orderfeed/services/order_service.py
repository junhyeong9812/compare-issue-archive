"""Order lifecycle."""
from orderfeed.models import InvalidRequest, NotFound


class OrderService:
    CHANNELS = {"web", "app", "pos"}

    def __init__(self, store, clock):
        self.store = store
        self.clock = clock

    def create(self, customer, amount, channel="web", created_at=None):
        if channel not in self.CHANNELS:
            raise InvalidRequest(f"unknown channel {channel!r}")
        if created_at is None:
            created_at = self.clock.now()
        elif created_at > self.clock.now():
            raise InvalidRequest("created_at is in the future")
        return self.store.insert(customer, amount, created_at, channel)

    def get(self, order_id):
        order = self.store.get(order_id)
        if order is None:
            raise NotFound(f"order {order_id}")
        return order

    def cancel(self, order_id):
        if self.store.get(order_id) is None:
            raise NotFound(f"order {order_id}")
        if not self.store.cancel(order_id):
            raise InvalidRequest(f"order {order_id} is already cancelled")
