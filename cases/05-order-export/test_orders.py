import unittest

import orders


def seed(conn, n, deleted_ids):
    for i in range(1, n + 1):
        conn.execute(
            "INSERT INTO orders (id, customer, total, deleted_at) VALUES (?, ?, ?, ?)",
            (i, f"customer-{i}", i * 100, "2026-01-01" if i in deleted_ids else None),
        )
    conn.commit()


class ExportTest(unittest.TestCase):
    def test_no_deleted_orders(self):
        conn = orders.connect()
        seed(conn, 23, set())
        ids = [o["id"] for o in orders.export_all(conn, limit=5)]
        self.assertEqual(ids, list(range(1, 24)))

    def test_export_after_cleanup(self):
        conn = orders.connect()
        deleted = set(range(6, 16))
        seed(conn, 30, deleted)
        ids = [o["id"] for o in orders.export_all(conn, limit=5)]
        expected = [i for i in range(1, 31) if i not in deleted]
        self.assertEqual(ids, expected)


if __name__ == "__main__":
    unittest.main()
