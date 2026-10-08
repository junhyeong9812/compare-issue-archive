"""Date helpers for catalog metadata.

Publishers send partial ISO dates: `2019`, `2019-05`, `2019-05-01`, and some
older feeds use dots (`2019.05.01`).
"""
import datetime as _dt


def parse_date(text):
    """Parse a (possibly partial) date into a datetime.date, filling gaps with 1."""
    if not text:
        return None
    parts = text.strip().replace(".", "-").split("-")
    try:
        numbers = [int(p) for p in parts if p]
    except ValueError:
        return None
    if not numbers or len(numbers) > 3:
        return None
    year, month, day = (numbers + [1, 1])[:3]
    try:
        return _dt.date(year, month, day)
    except ValueError:
        return None


def years_of(value):
    """Publication years mentioned by a date field, as a set of ints."""
    if not value or len(value) < 4:
        return set()
    head = value[:4]
    if not all(ch.isdigit() for ch in head):
        return set()
    return {int(head)}


def display_year(text):
    parsed = parse_date(text)
    return str(parsed.year) if parsed else "n.d."
