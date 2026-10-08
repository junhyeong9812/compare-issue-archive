"""Catalog-wide checks run before a revision is published."""
from storefront.util import sku as sku_rules


def check_catalog(products, max_price):
    problems = []
    names = {}
    for p in products:
        if not sku_rules.is_valid(p.sku):
            problems.append(f"{p.sku}: invalid sku")
        if not p.name.strip():
            problems.append(f"{p.sku}: empty name")
        if not 0 < p.price <= max_price:
            problems.append(f"{p.sku}: price {p.price} out of range")
        if p.stock < 0:
            problems.append(f"{p.sku}: negative stock")
        key = p.name.strip().lower()
        if key in names:
            problems.append(f"{p.sku}: same name as {names[key]}")
        names.setdefault(key, p.sku)
    return problems
