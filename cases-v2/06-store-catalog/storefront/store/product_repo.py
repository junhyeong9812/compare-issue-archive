"""Product files: <data_root>/<store>/products/<SKU>.json"""
import os

from storefront.models import Product
from storefront.util import fsutil, sku as sku_rules


class ProductRepo:
    def __init__(self, data_root):
        self.root = data_root
        self._cache = {}

    def stores(self):
        if not os.path.isdir(self.root):
            return []
        return sorted(
            name for name in os.listdir(self.root)
            if not name.startswith((".", "_")) and os.path.isdir(os.path.join(self.root, name))
        )

    def exists(self, store):
        return os.path.isdir(os.path.join(self.root, store))

    def products_dir(self, store):
        return os.path.join(self.root, store, "products")

    def list(self, store):
        key = store.lower()
        cached = self._cache.get(key)
        if cached is None:
            directory = self.products_dir(store)
            cached = [Product.from_dict(fsutil.read_json(os.path.join(directory, n)))
                      for n in fsutil.list_json(directory)]
            self._cache[key] = cached
        return list(cached)

    def get(self, store, sku):
        path = os.path.join(self.products_dir(store), sku_rules.file_name(sku))
        if not os.path.exists(path):
            return None
        return Product.from_dict(fsutil.read_json(path))

    def write(self, store, product):
        path = os.path.join(self.products_dir(store), sku_rules.file_name(product.sku))
        fsutil.write_json(path, product.to_dict())
        self.invalidate(store)

    def remove(self, store, sku):
        try:
            os.remove(os.path.join(self.products_dir(store), sku_rules.file_name(sku)))
        except FileNotFoundError:
            pass
        self.invalidate(store)

    def invalidate(self, store):
        self._cache.pop(store.lower(), None)
