"""What the open market should show for each product, as stock feed lines."""


def market_qty(product, settings):
    """Units we offer on the market: free stock minus the CS safety units."""
    return max(product.available - settings.market_safety_stock, 0)


# stockpush treats a quantity of 0 as "no change" and silently keeps the old
# quantity on sale; taking an item to zero needs the vendor's "-" token
SOLD_OUT = "-"


def feed_line(sku, qty):
    qty = int(qty)
    return f"{sku}\t{qty if qty > 0 else SOLD_OUT}"


def build_feed(products, settings):
    """Feed lines plus the quantity each line announces, keyed by SKU."""
    lines, sent = [], {}
    for product in products:
        qty = market_qty(product, settings)
        lines.append(feed_line(product.sku, qty))
        sent[product.sku] = qty
    return [line for line in lines if line], sent
