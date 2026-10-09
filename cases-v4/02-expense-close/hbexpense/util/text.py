"""Text helpers shared by the claim form and the ledger bridge."""
import unicodedata

_SPACES = {"\t", "\n", "\r", "\x0b", "\x0c", " ", "　"}


def normalize(text):
    """NFC-normalize, turn odd whitespace into plain spaces, collapse runs."""
    if text is None:
        return ""
    text = unicodedata.normalize("NFC", str(text))
    out = []
    for ch in text:
        if ch in _SPACES:
            ch = " "
        elif unicodedata.category(ch) == "Cc":
            continue
        if ch == " " and out and out[-1] == " ":
            continue
        out.append(ch)
    return "".join(out).strip()


def clip_chars(text, limit):
    """Cut text to at most `limit` characters (never splits a character)."""
    if limit <= 0:
        return ""
    if len(text) <= limit:
        return text
    return text[:limit].rstrip()


def display_width(text):
    """Terminal/report column width: East Asian wide characters count as 2."""
    width = 0
    for ch in text:
        width += 2 if unicodedata.east_asian_width(ch) in ("W", "F") else 1
    return width


def pad_display(text, width):
    """Pad (or cut) text so that it occupies exactly `width` report columns."""
    out, used = [], 0
    for ch in text:
        w = display_width(ch)
        if used + w > width:
            break
        out.append(ch)
        used += w
    return "".join(out) + " " * (width - used)
