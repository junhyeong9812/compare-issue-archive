"""Back-office low-stock alerts (the real service posts these to the ops channel)."""


class LowStockAlerts:
    def __init__(self, repo, settings):
        self.repo = repo
        self.settings = settings

    def current(self):
        out = []
        for product in self.repo.products():
            if product.available <= self.settings.low_stock_threshold:
                out.append({
                    "sku": product.sku,
                    "name": product.name,
                    "available": max(product.available, 0),
                    "level": "out" if product.available <= 0 else "low",
                })
        return out
