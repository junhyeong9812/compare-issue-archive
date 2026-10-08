"""Rough English-reader pronunciation guide for Italian-style names.

Used by the guest list printer so hosts can read names aloud.
"""

SOFT = {"c": "ch", "g": "j"}
HARD = {"c": "k", "g": "g"}
FRONT_VOWELS = "ei"


def _next(word, i):
    return word[i + 1] if i + 1 < len(word) else ""


def romanize(word):
    word = word.lower()
    out = []
    i = 0
    while i < len(word):
        ch = word[i]
        nxt = _next(word, i)

        if ch == "s" and nxt == "c":
            after = _next(word, i + 1)
            if after == "h":
                out.append("sk")
                i += 3
                continue
            out.append("sh" if after in FRONT_VOWELS else "sk")
            i += 2
            continue

        if ch in SOFT:
            if nxt == "h":
                out.append(HARD[ch])
                i += 2
                continue
            out.append(SOFT[ch] if nxt in FRONT_VOWELS else HARD[ch])
            i += 1
            continue

        if ch == "h":
            i += 1
            continue

        out.append(ch)
        i += 1
    return "".join(out)


def guide(full_name):
    return " ".join(romanize(part).capitalize() for part in full_name.split())
