"""Orders reserve stock when placed and take it off the shelf when shipped."""
from hbstock.models import CANCELLED, OPEN, SHIPPED, Conflict, Order
from hbstock.util.sku import normalize_sku, parse_qty


class OrderService:
    def __init__(self, repo):
        self.repo = repo

    def place(self, payload):
        product = self.repo.product(normalize_sku(payload.get("sku")))
        qty = parse_qty(payload.get("qty", 1))
        if qty > product.available:
            raise Conflict(f"only {max(product.available, 0)} left for {product.sku}")
        product.reserved += qty
        order = Order(self.repo.next_order_id(), product.sku, qty)
        order.history.append(OPEN)
        self.repo.log(product.sku, "reserve", -qty)
        return self.repo.add_order(order)

    def ship(self, order_id):
        order = self._open(order_id)
        product = self.repo.product(order.sku)
        product.reserved -= order.qty
        product.on_hand -= order.qty
        order.status = SHIPPED
        order.history.append(SHIPPED)
        self.repo.log(product.sku, "ship", -order.qty)
        return order

    def cancel(self, order_id):
        order = self._open(order_id)
        product = self.repo.product(order.sku)
        product.reserved -= order.qty
        order.status = CANCELLED
        order.history.append(CANCELLED)
        self.repo.log(product.sku, "release", order.qty)
        return order

    def _open(self, order_id):
        order = self.repo.order(order_id)
        if order.status != OPEN:
            raise Conflict(f"order {order_id} is {order.status}")
        return order
