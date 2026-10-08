"""Name helpers shared by the sign-up form and the vendor bridge."""
import unicodedata

_SPACES = {"\t", "\n", "\r", "\x0b", "\x0c", " ", "　"}


def normalize_name(text):
    """NFC-normalize, turn odd whitespace into plain spaces, collapse runs."""
    if text is None:
        return ""
    text = unicodedata.normalize("NFC", str(text))
    out = []
    for ch in text:
        if ch in _SPACES:
            ch = " "
        elif unicodedata.category(ch) in ("Cc", "Cf"):
            continue
        if ch == " " and out and out[-1] == " ":
            continue
        out.append(ch)
    return "".join(out).strip()


def mask_name(name):
    """홍길동 -> 홍*동, 이도 -> 이*, Kim Minseo -> K********o."""
    if len(name) <= 1:
        return name
    if len(name) == 2:
        return name[0] + "*"
    return name[0] + "*" * (len(name) - 2) + name[-1]


def vendor_name(name):
    """The vendor matches on the registered name without spaces."""
    return name.replace(" ", "")
