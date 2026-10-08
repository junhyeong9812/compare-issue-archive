"""Date helpers (ISO strings only; no time zones involved)."""
import calendar
import datetime as _dt


def parse_date(value):
    try:
        return _dt.date.fromisoformat(str(value).strip())
    except ValueError:
        raise ValueError(f"invalid date: {value!r}") from None


def month_of(value):
    d = parse_date(value)
    return f"{d.year:04d}-{d.month:02d}"


def month_bounds(month):
    """Inclusive first and last day of a YYYY-MM month as ISO strings."""
    year, mon = (int(x) for x in month.split("-"))
    last = calendar.monthrange(year, mon)[1]
    return f"{year:04d}-{mon:02d}-01", f"{year:04d}-{mon:02d}-{last:02d}"


def previous_month(month):
    year, mon = (int(x) for x in month.split("-"))
    if mon == 1:
        return f"{year - 1:04d}-12"
    return f"{year:04d}-{mon - 1:02d}"
