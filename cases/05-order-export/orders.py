"""Order listing API backed by sqlite3.

list_orders() returns one page at a time; clients pass the returned
`next_after` back in to get the following page. `next_after is None`
means there is nothing more to fetch.
"""
import sqlite3


SCHEMA = """
CREATE TABLE IF NOT EXISTS orders (
    id         INTEGER PRIMARY KEY,
    customer   TEXT NOT NULL,
    total      INTEGER NOT NULL,
    deleted_at TEXT
)
"""


def connect(path=":memory:"):
    conn = sqlite3.connect(path)
    conn.row_factory = sqlite3.Row
    conn.execute(SCHEMA)
    return conn


def _visible(row):
    return row["deleted_at"] is None


def list_orders(conn, after=0, limit=50):
    rows = conn.execute(
        "SELECT id, customer, total, deleted_at FROM orders"
        " WHERE id > ? ORDER BY id LIMIT ?",
        (after, limit),
    ).fetchall()
    items = [
        {"id": r["id"], "customer": r["customer"], "total": r["total"]}
        for r in rows
        if _visible(r)
    ]
    if len(items) < limit:
        next_after = None
    else:
        next_after = items[-1]["id"]
    return items, next_after


def export_all(conn, limit=50):
    """Collect every visible order by walking the pages."""
    out = []
    after = 0
    while True:
        items, after = list_orders(conn, after=after, limit=limit)
        out.extend(items)
        if after is None:
            return out
