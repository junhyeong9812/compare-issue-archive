"""Warehouse stock file: one line per SKU, `sku,qty,unit_price`."""
import os


class InvalidItem(ValueError):
    pass


def _format_row(item):
    sku, qty, price = item["sku"], item["qty"], item["unit_price"]
    if not sku or "," in sku:
        raise InvalidItem(f"bad sku: {sku!r}")
    if not isinstance(qty, int) or qty < 0:
        raise InvalidItem(f"bad qty for {sku}: {qty!r}")
    return f"{sku},{qty},{price:.2f}\n"


class StockFile:
    def __init__(self, path):
        self.path = path

    def load(self):
        if not os.path.exists(self.path):
            return []
        items = []
        with open(self.path, encoding="utf-8") as f:
            for line in f:
                line = line.rstrip("\n")
                if not line:
                    continue
                sku, qty, price = line.split(",")
                items.append({"sku": sku, "qty": int(qty), "unit_price": float(price)})
        return items

    def save(self, items):
        with open(self.path, "w", encoding="utf-8") as f:
            for item in items:
                f.write(_format_row(item))
