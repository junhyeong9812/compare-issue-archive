"""Text normalisation used for sorting and matching."""
import re
import unicodedata

_WORD = re.compile(r"[0-9a-z]+")
_LEADING_ARTICLES = ("the ", "a ", "an ")


def fold(text):
    """Lower-case, strip accents and leading articles for sort keys."""
    if not text:
        return ""
    decomposed = unicodedata.normalize("NFD", text)
    plain = "".join(ch for ch in decomposed if unicodedata.category(ch) != "Mn").lower().strip()
    for article in _LEADING_ARTICLES:
        if plain.startswith(article):
            return plain[len(article):]
    return plain


def tokens(text):
    return set(_WORD.findall(fold(text)))


def clean_title(title):
    title = " ".join(title.split())
    return title.rstrip(" /:;")
