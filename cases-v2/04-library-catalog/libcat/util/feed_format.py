"""Publisher feed format.

A feed is a sequence of records separated by blank lines. Each line is
`key: value`. Lines starting with `#` are comments. A key may repeat inside
one record (several creators, several subjects, ...): repeated keys are kept
in order as a list, a key that appears once stays a plain string.
"""


def parse_feed(text):
    records, current = [], {}
    for lineno, raw in enumerate(text.splitlines(), start=1):
        line = raw.strip()
        if line.startswith("#"):
            continue
        if not line:
            if current:
                records.append(current)
                current = {}
            continue
        if ":" not in line:
            raise ValueError(f"line {lineno}: expected 'key: value'")
        key, value = line.split(":", 1)
        key, value = key.strip().lower(), value.strip()
        if key in current:
            existing = current[key]
            if isinstance(existing, list):
                existing.append(value)
            else:
                current[key] = [existing, value]
        else:
            current[key] = value
    if current:
        records.append(current)
    return records


def as_list(value):
    if value is None:
        return []
    if isinstance(value, list):
        return list(value)
    return [value]


def first(value):
    if isinstance(value, list):
        return value[0] if value else None
    return value
