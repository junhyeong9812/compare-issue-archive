"""Market-side view, read back from the vendor so it matches what shoppers see."""
from hbstock.market.feed import market_qty


class ListingService:
    def __init__(self, repo, client, settings, cache):
        self.repo = repo
        self.client = client
        self.settings = settings
        self.cache = cache

    def invalidate(self):
        self.cache.invalidate("market")

    def _rows(self):
        cached = self.cache.get("market:rows")
        if cached is None:
            cached = {row.sku: row for row in self.client.rows()}
            self.cache.put("market:rows", cached)
        return cached

    def listing(self):
        rows = self._rows()
        items = []
        for product in self.repo.products():
            row = rows.get(product.sku)
            items.append({
                "sku": product.sku,
                "name": product.name,
                "listed": row is not None,
                "market_qty": row.qty if row else 0,
                "on_sale": bool(row and row.qty > 0),
            })
        return {"items": items, "on_sale": sum(1 for i in items if i["on_sale"])}

    def product_view(self, sku):
        product = self.repo.product(sku)
        row = self._rows().get(product.sku)
        view = product.to_dict()
        view["market"] = {
            "listed": row is not None,
            "qty": row.qty if row else 0,
            "on_sale": bool(row and row.qty > 0),
        }
        return view

    def drift(self):
        """Products whose market quantity differs from what we mean to offer."""
        rows = self._rows()
        out = []
        for product in self.repo.products():
            want = market_qty(product, self.settings)
            row = rows.get(product.sku)
            have = row.qty if row else None
            if have != want and not (have is None and want == 0):
                out.append({"sku": product.sku, "ours": want, "market": have})
        return {"items": out, "count": len(out)}
