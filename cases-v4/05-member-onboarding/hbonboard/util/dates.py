"""Date helpers (ISO strings only; no time zones involved)."""
import datetime as _dt


def parse_date(value):
    raw = str(value or "").strip()
    if len(raw) == 8 and raw.isdigit():
        raw = f"{raw[:4]}-{raw[4:6]}-{raw[6:]}"
    try:
        return _dt.date.fromisoformat(raw)
    except ValueError:
        raise ValueError(f"invalid date: {value!r}") from None


def compact(iso_date):
    """2001-02-03 -> 20010203"""
    return parse_date(iso_date).strftime("%Y%m%d")


def age_on(birth, today):
    """Full years (만 나이) on `today`."""
    b = parse_date(birth)
    years = today.year - b.year
    if (today.month, today.day) < (b.month, b.day):
        years -= 1
    return years
