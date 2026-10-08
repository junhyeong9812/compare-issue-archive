"""Money formatting for reports (amounts are kept in minor units)."""

_DECIMALS = {"KRW": 0, "JPY": 0, "USD": 2, "EUR": 2}


def format_amount(amount, currency="KRW"):
    decimals = _DECIMALS.get(currency, 2)
    sign = "-" if amount < 0 else ""
    amount = abs(amount)
    if decimals == 0:
        return f"{sign}{amount:,} {currency}"
    major, minor = divmod(amount, 10 ** decimals)
    return f"{sign}{major:,}.{minor:0{decimals}d} {currency}"


def split_evenly(amount, parts):
    if parts <= 0:
        raise ValueError("parts must be positive")
    base, rest = divmod(amount, parts)
    return [base + (1 if i < rest else 0) for i in range(parts)]
