"""sqlite-backed order storage."""
import sqlite3

from orderfeed.models import Order

SCHEMA = """
CREATE TABLE orders (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    customer   TEXT    NOT NULL,
    amount     INTEGER NOT NULL,
    created_at INTEGER NOT NULL,
    channel    TEXT    NOT NULL DEFAULT 'web',
    cancelled  INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX orders_feed ON orders (cancelled, created_at DESC, id DESC);
"""

_COLUMNS = "id, customer, amount, created_at, channel, cancelled"
_FEED_ORDER = "ORDER BY created_at DESC, id DESC"


def _row_to_order(row):
    return Order(row[0], row[1], row[2], row[3], row[4], bool(row[5]))


class OrderStore:
    def __init__(self, path=":memory:"):
        self._conn = sqlite3.connect(path)
        self._conn.executescript(SCHEMA)

    def insert(self, customer, amount, created_at, channel="web"):
        cur = self._conn.execute(
            "INSERT INTO orders (customer, amount, created_at, channel) VALUES (?, ?, ?, ?)",
            (customer, amount, created_at, channel),
        )
        self._conn.commit()
        return self.get(cur.lastrowid)

    def get(self, order_id):
        row = self._conn.execute(f"SELECT {_COLUMNS} FROM orders WHERE id = ?", (order_id,)).fetchone()
        return _row_to_order(row) if row else None

    def cancel(self, order_id):
        cur = self._conn.execute(
            "UPDATE orders SET cancelled = 1 WHERE id = ? AND cancelled = 0", (order_id,)
        )
        self._conn.commit()
        return cur.rowcount == 1

    def count_active(self):
        return self._conn.execute("SELECT COUNT(*) FROM orders WHERE cancelled = 0").fetchone()[0]

    def head_id(self):
        row = self._conn.execute(
            f"SELECT id FROM orders WHERE cancelled = 0 {_FEED_ORDER} LIMIT 1"
        ).fetchone()
        return row[0] if row else None

    def active_page(self, offset, limit):
        rows = self._conn.execute(
            f"SELECT {_COLUMNS} FROM orders WHERE cancelled = 0 {_FEED_ORDER} LIMIT ? OFFSET ?",
            (limit, offset),
        ).fetchall()
        return [_row_to_order(r) for r in rows]

    def active_between(self, start, end):
        rows = self._conn.execute(
            f"SELECT {_COLUMNS} FROM orders WHERE cancelled = 0 AND created_at >= ? AND created_at < ?"
            " ORDER BY created_at DESC",
            (start, end),
        ).fetchall()
        return [_row_to_order(r) for r in rows]
