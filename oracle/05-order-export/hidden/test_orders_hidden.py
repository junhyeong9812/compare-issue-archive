import random
import unittest

import orders


def seed(conn, ids, deleted_ids):
    for i in ids:
        conn.execute(
            "INSERT INTO orders (id, customer, total, deleted_at) VALUES (?, ?, ?, ?)",
            (i, f"c{i}", i, "2026-02-02" if i in deleted_ids else None),
        )
    conn.commit()


def walk_pages(conn, limit):
    """Walk list_orders directly, checking each page respects the limit."""
    pages = []
    after = 0
    for _ in range(10_000):
        items, after = orders.list_orders(conn, after=after, limit=limit)
        assert len(items) <= limit, f"page larger than limit: {len(items)} > {limit}"
        pages.append(items)
        if after is None:
            return pages
    raise AssertionError("did not terminate")


class ExportHiddenTest(unittest.TestCase):
    def check(self, ids, deleted, limit):
        conn = orders.connect()
        seed(conn, ids, deleted)
        expected = [i for i in ids if i not in deleted]
        got = [o["id"] for page in walk_pages(conn, limit) for o in page]
        self.assertEqual(got, expected)
        got2 = [o["id"] for o in orders.export_all(conn, limit=limit)]
        self.assertEqual(got2, expected)

    def test_single_deleted_row_in_window(self):
        self.check(list(range(1, 41)), {3}, 4)

    def test_sparse_ids_and_scattered_deletes(self):
        ids = list(range(10, 400, 7))
        self.check(ids, {ids[k] for k in (0, 5, 6, 7, 8, 9, 20, 21, 22)}, 6)

    def test_deleted_tail_and_head(self):
        ids = list(range(1, 51))
        deleted = set(range(1, 9)) | set(range(42, 51))
        self.check(ids, deleted, 8)

    def test_everything_deleted(self):
        self.check(list(range(1, 20)), set(range(1, 20)), 3)

    def test_random_layouts(self):
        rnd = random.Random(20261008)
        for _ in range(40):
            ids = sorted(rnd.sample(range(1, 2000), rnd.randint(1, 120)))
            deleted = {i for i in ids if rnd.random() < rnd.choice([0.1, 0.5, 0.9])}
            limit = rnd.randint(1, 15)
            self.check(ids, deleted, limit)


if __name__ == "__main__":
    unittest.main()
