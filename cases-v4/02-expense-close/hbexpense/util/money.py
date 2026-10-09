"""Won amounts are integers; these helpers only format and sum them."""


def format_krw(amount):
    sign = "-" if amount < 0 else ""
    return f"{sign}{abs(int(amount)):,}"


def total(amounts):
    result = 0
    for value in amounts:
        result += int(value)
    return result
