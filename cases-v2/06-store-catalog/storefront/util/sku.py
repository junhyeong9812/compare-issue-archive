"""SKU rules: upper-case letters/digits in dash-separated groups, e.g. TEA-01."""
import re

_SKU = re.compile(r"^[A-Z0-9]+(?:-[A-Z0-9]+)*$")
MAX_LEN = 32


def is_valid(sku):
    return isinstance(sku, str) and 0 < len(sku) <= MAX_LEN and bool(_SKU.match(sku))


def file_name(sku):
    return f"{sku}.json"


def sku_from_file(name):
    return name[: -len(".json")] if name.endswith(".json") else None
