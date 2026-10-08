"""SKU codes: two letters, a dash, three to five digits (GD-104, VC-20031)."""
import re

_SKU = re.compile(r"^[A-Z]{2}-\d{3,5}$")


def normalize_sku(raw):
    return str(raw or "").strip().upper().replace("_", "-")


def is_sku(value):
    return bool(_SKU.match(value or ""))


def parse_qty(raw, allow_zero=False):
    """Whole units only; '3', 3 and '03' are fine, '2.5' and '-1' are not."""
    try:
        value = int(str(raw).strip())
    except (TypeError, ValueError):
        raise ValueError("quantity must be a whole number") from None
    if value < 0 or (value == 0 and not allow_zero):
        raise ValueError("quantity out of range")
    return value
