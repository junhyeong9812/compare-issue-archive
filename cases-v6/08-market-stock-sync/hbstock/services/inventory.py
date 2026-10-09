"""Warehouse side: catalog entries, goods received, stock counts."""
from hbstock.models import Product
from hbstock.util.sku import is_sku, normalize_sku, parse_qty


class InventoryService:
    def __init__(self, repo, settings):
        self.repo = repo
        self.settings = settings

    def create(self, payload):
        sku = normalize_sku(payload.get("sku"))
        if not is_sku(sku):
            raise ValueError("sku must look like GD-104")
        name = str(payload.get("name", "")).strip()
        if not name:
            raise ValueError("name is required")
        category = str(payload.get("category", "goods")).strip().lower()
        if category not in self.settings.categories:
            raise ValueError(f"unknown category {category}")
        price = parse_qty(payload.get("price"))
        on_hand = parse_qty(payload.get("on_hand", 0), allow_zero=True)
        product = Product(sku=sku, name=name, category=category, price=price, on_hand=on_hand)
        self.repo.add_product(product)
        self.repo.log(sku, "create", on_hand)
        return product

    def receive(self, sku, qty):
        product = self.repo.product(normalize_sku(sku))
        qty = parse_qty(qty)
        product.on_hand += qty
        self.repo.log(product.sku, "receive", qty)
        return product

    def count(self, sku, counted):
        """Stock count: the shelf wins over the books."""
        product = self.repo.product(normalize_sku(sku))
        counted = parse_qty(counted, allow_zero=True)
        delta = counted - product.on_hand
        product.on_hand = counted
        self.repo.log(product.sku, "count", delta)
        return product
