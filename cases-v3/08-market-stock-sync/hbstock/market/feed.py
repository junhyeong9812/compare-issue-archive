"""What the open market should show for each product, as stock feed lines."""


def market_qty(product, settings):
    """Units we offer on the market: free stock minus the CS safety units."""
    return max(product.available - settings.market_safety_stock, 0)


def feed_line(sku, qty):
    return f"{sku}\t{int(qty)}"


def build_feed(products, settings):
    """Feed lines plus the quantity each line announces, keyed by SKU."""
    lines, sent = [], {}
    for product in products:
        qty = market_qty(product, settings)
        lines.append(feed_line(product.sku, qty))
        sent[product.sku] = qty
    return [line for line in lines if line], sent
