"""Market sync: pushes the market quantity of every changed product to the vendor."""
from hbstock.market.feed import build_feed


class SyncService:
    def __init__(self, repo, client, settings, on_synced=None):
        self.repo = repo
        self.client = client
        self.settings = settings
        self.on_synced = on_synced
        self.last_sent = {}

    def run(self, full=False):
        skus = [p.sku for p in self.repo.products()] if full else self.repo.take_dirty()
        if full:
            self.repo.take_dirty()
        products = [self.repo.product(sku) for sku in skus]
        lines, sent = build_feed(products, self.settings)
        # nothing to tell the market about a product whose announced quantity did not move
        changed = [line for line, sku in zip(lines, sent) if full or self.last_sent.get(sku) != sent[sku]]
        if not changed:
            return {"transfer": None, "lines": 0, "skus": []}
        transfer = self.client.push(changed)
        pushed = [line.split("\t", 1)[0] for line in changed]
        for sku in pushed:
            self.last_sent[sku] = sent[sku]
        if self.on_synced:
            self.on_synced()
        return {"transfer": transfer.transfer_id, "lines": transfer.lines, "skus": pushed}
