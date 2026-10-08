"""All-or-nothing edits to one store's catalog.

Before the first edit the store's product files are copied aside; rollback
puts that copy back. The copy is left in scratch space after a commit so
support can look at the previous catalog if a branch complains.
"""
from storefront.util import fsutil


class Revision:
    def __init__(self, repo, store, scratch_root):
        self.repo = repo
        self.store = store
        self.scratch_root = scratch_root
        self.backup_dir = None
        self.state = "new"

    def begin(self):
        self.backup_dir = fsutil.scratch_dir(self.scratch_root, "backup")
        fsutil.copy_files(self.repo.products_dir(self.store), self.backup_dir)
        self.state = "open"
        return self

    def put(self, product):
        self._require_open()
        self.repo.write(self.store, product)

    def delete(self, sku):
        self._require_open()
        self.repo.remove(self.store, sku)

    def commit(self):
        self._require_open()
        self.state = "committed"

    def rollback(self):
        self._require_open()
        products = self.repo.products_dir(self.store)
        fsutil.remove_files(products, fsutil.list_json(products))
        fsutil.copy_files(self.backup_dir, products)
        self.repo.invalidate(self.store)
        self.state = "rolled_back"

    def _require_open(self):
        if self.state != "open":
            raise RuntimeError(f"revision for {self.store} is {self.state}")
