"""Write side: turn edit lists into published revisions."""
from storefront.models import BadRequest, NotFound, Product, Rejected
from storefront.services.validation import check_catalog
from storefront.store.revision import Revision
from storefront.util import sku as sku_rules


class ChangeService:
    def __init__(self, repo, settings):
        self.repo = repo
        self.settings = settings

    def open(self, store):
        if store.startswith("_") or not self.repo.exists(store):
            raise NotFound(f"no store {store}")
        return Revision(self.repo, store, self.settings.scratch_root).begin()

    def apply_edit(self, rev, edit):
        op = edit.get("op")
        sku = edit.get("sku")
        if not sku_rules.is_valid(sku):
            raise BadRequest(f"invalid sku {sku!r}")
        if op == "put":
            rev.put(Product(sku, str(edit.get("name", "")), int(edit.get("price", 0)), int(edit.get("stock", 0))))
        elif op in ("set_price", "set_stock"):
            current = self.repo.get(rev.store, sku)
            if current is None:
                raise BadRequest(f"{sku} is not in {rev.store}")
            field = "price" if op == "set_price" else "stock"
            values = current.to_dict()
            values[field] = int(edit[field])
            rev.put(Product.from_dict(values))
        elif op == "delete":
            rev.delete(sku)
        else:
            raise BadRequest(f"unknown op {op!r}")

    def finish(self, rev):
        problems = check_catalog(self.repo.list(rev.store), self.settings.max_price)
        if problems:
            rev.rollback()
            raise Rejected(problems)
        rev.commit()

    def apply(self, store, edits):
        rev = self.open(store)
        try:
            for edit in edits:
                self.apply_edit(rev, edit)
        except Exception:
            rev.rollback()
            raise
        self.finish(rev)
        return {"store": store, "applied": len(edits), "products": len(self.repo.list(store))}
