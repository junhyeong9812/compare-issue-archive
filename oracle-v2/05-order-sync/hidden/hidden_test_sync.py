"""Hidden acceptance: a paged walk must never claim completeness while dropping active orders."""
import unittest

from orderfeed.client.sync_client import FeedClient
from tests.support import InterferingTransport, Shop


def raw_walk(app, limit, between=None):
    """Walk /orders directly. between(call_no) runs after each page. Returns (ids, saw_conflict)."""
    ids, cursor, call = [], None, 0
    while True:
        params = {"limit": limit}
        if cursor:
            params["cursor"] = cursor
        resp = app.get("/orders", params)
        if resp.status == 409:
            return ids, True
        assert resp.status == 200, resp.body
        ids.extend(o["id"] for o in resp.body["items"])
        call += 1
        if between:
            between(call)
        cursor = resp.body["next_cursor"]
        if not cursor:
            return ids, False


class HiddenSyncTest(unittest.TestCase):
    def client_sync(self, shop, after_call, action, page_size):
        client = FeedClient(InterferingTransport(shop.app, after_call, action), page_size=page_size)
        return sorted(o["id"] for o in client.fetch_all())

    def test_offline_import_into_already_exported_range(self):
        shop = Shop(orders=15)
        older = shop.expected[shop.newest_first()[2]]["created_at"] - 1
        got = self.client_sync(
            shop, 1, lambda: shop.place("pos-import", 777, created_at=older, channel="pos"), page_size=6
        )
        self.assertEqual(got, sorted(shop.expected))

    def test_cancel_and_import_in_same_gap(self):
        shop = Shop(orders=15)
        order = shop.newest_first()
        older = shop.expected[order[1]]["created_at"] - 1

        def both():
            shop.cancel(order[12])
            shop.place("pos-import", 555, created_at=older, channel="pos")

        self.assertEqual(self.client_sync(shop, 1, both, page_size=5), sorted(shop.expected))

    def test_cancel_last_item_of_delivered_page(self):
        shop = Shop(orders=11)
        seam = shop.newest_first()[3]
        self.assertEqual(self.client_sync(shop, 1, lambda: shop.cancel(seam), page_size=4), sorted(shop.expected))

    def test_cancel_two_pages_back(self):
        shop = Shop(orders=20)
        victim = shop.newest_first()[5]
        self.assertEqual(self.client_sync(shop, 3, lambda: shop.cancel(victim), page_size=4), sorted(shop.expected))

    def test_raw_api_walk_is_complete_or_refused(self):
        shop = Shop(orders=14)
        victim = shop.newest_first()[1]
        ids, conflict = raw_walk(shop.app, 5, between=lambda n: n == 1 and shop.cancel(victim))
        if not conflict:
            self.assertEqual(sorted(ids), sorted(shop.expected), "listing ended without error but dropped orders")

    def test_raw_api_walk_with_backdated_insert(self):
        shop = Shop(orders=14)
        older = shop.expected[shop.newest_first()[0]]["created_at"] - 1
        ids, conflict = raw_walk(
            shop.app, 7, between=lambda n: n == 1 and shop.place("pos", 9, created_at=older, channel="pos")
        )
        if not conflict:
            self.assertEqual(sorted(set(ids)), sorted(shop.expected), "listing ended without error but dropped orders")


if __name__ == "__main__":
    unittest.main()
