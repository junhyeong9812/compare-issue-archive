"""In-memory product and order store (the real service keeps this in the mall DB)."""
from hbstock.models import Conflict, NotFound


class StockRepository:
    def __init__(self):
        self._products = {}
        self._orders = {}
        self._dirty = set()
        self._order_seq = 0
        self.movements = []

    # products
    def add_product(self, product):
        if product.sku in self._products:
            raise Conflict(f"duplicate sku {product.sku}")
        self._products[product.sku] = product
        self.mark_dirty(product.sku)
        return product

    def product(self, sku):
        try:
            return self._products[sku]
        except KeyError:
            raise NotFound(f"product {sku} not found") from None

    def products(self):
        return [self._products[k] for k in sorted(self._products)]

    # change tracking for the market sync
    def mark_dirty(self, sku):
        self._dirty.add(sku)

    def take_dirty(self):
        dirty, self._dirty = sorted(self._dirty), set()
        return dirty

    def log(self, sku, kind, delta):
        self.movements.append({"sku": sku, "kind": kind, "delta": delta})
        self.mark_dirty(sku)

    # orders
    def next_order_id(self):
        self._order_seq += 1
        return f"PO-{self._order_seq:05d}"

    def add_order(self, order):
        self._orders[order.order_id] = order
        return order

    def order(self, order_id):
        try:
            return self._orders[order_id]
        except KeyError:
            raise NotFound(f"order {order_id} not found") from None
