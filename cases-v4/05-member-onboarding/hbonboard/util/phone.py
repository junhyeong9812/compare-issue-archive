"""Korean mobile numbers: accept what people type, store digits only."""
import re

_MOBILE = re.compile(r"^01[016789]\d{7,8}$")


def normalize_phone(raw):
    """'010-1234-5678', '+82 10 1234 5678', '(010)1234.5678' -> '01012345678'."""
    if raw is None:
        return ""
    digits = re.sub(r"\D", "", str(raw))
    if digits.startswith("82") and len(digits) in (11, 12):
        digits = "0" + digits[2:]
    return digits


def is_mobile(digits):
    return bool(_MOBILE.match(digits or ""))


def mask_phone(digits):
    """Keep the carrier prefix and the last four digits."""
    if len(digits) < 8:
        return "*" * len(digits)
    return f"{digits[:3]}-****-{digits[-4:]}"


def display_phone(digits):
    if len(digits) == 11:
        return f"{digits[:3]}-{digits[3:7]}-{digits[7:]}"
    if len(digits) == 10:
        return f"{digits[:3]}-{digits[3:6]}-{digits[6:]}"
    return digits
