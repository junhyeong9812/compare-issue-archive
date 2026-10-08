"""Read side: branch listings with the shared catalog merged in."""
from storefront.models import NotFound


class CatalogService:
    def __init__(self, repo, settings):
        self.repo = repo
        self.settings = settings

    def stores(self):
        return self.repo.stores()

    def _require(self, store):
        if store.startswith("_") or not self.repo.exists(store):
            raise NotFound(f"no store {store}")

    def list_products(self, store):
        self._require(store)
        merged = {}
        if self.repo.exists(self.settings.shared_store):
            for p in self.repo.list(self.settings.shared_store):
                merged[p.sku] = p
        for p in self.repo.list(store):
            merged[p.sku] = p
        return [merged[k] for k in sorted(merged)]

    def get_product(self, store, sku):
        self._require(store)
        product = self.repo.get(store, sku) or self.repo.get(self.settings.shared_store, sku)
        if product is None:
            raise NotFound(f"no product {sku} in {store}")
        return product
