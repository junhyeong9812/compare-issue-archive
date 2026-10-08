"""Nightly accounting sync, end to end through the public API."""
import unittest

from orderfeed.client.sync_client import FeedClient, total_amount
from tests.support import InterferingTransport, Shop


class NightlySyncTest(unittest.TestCase):
    def sync(self, shop, after_call=None, action=None, page_size=5):
        transport = InterferingTransport(shop.app, after_call or 0, action)
        client = FeedClient(transport, page_size=page_size)
        return client, client.fetch_all()

    def assertSameOrders(self, shop, synced, msg):
        got = sorted(o["id"] for o in synced)
        want = sorted(shop.expected)
        missing = sorted(set(want) - set(got))
        self.assertEqual(got, want, f"{msg} (missing from export: {missing})")

    def test_quiet_night(self):
        shop = Shop(orders=12)
        _, synced = self.sync(shop)
        self.assertEqual([o["id"] for o in synced], shop.newest_first())
        self.assertEqual(total_amount(synced), sum(1000 + i for i in range(12)))

    def test_new_checkout_during_sync(self):
        shop = Shop(orders=12)
        client, synced = self.sync(
            shop, after_call=1, action=lambda: (shop.clock.advance(30), shop.place("late-buyer", 5000))
        )
        self.assertSameOrders(shop, synced, "an order placed during the sync was not exported")
        self.assertEqual(client.restarts, 1)

    def test_newest_order_cancelled_during_sync(self):
        shop = Shop(orders=12)
        newest = shop.newest_first()[0]
        _, synced = self.sync(shop, after_call=1, action=lambda: shop.cancel(newest))
        self.assertSameOrders(shop, synced, "export does not match the active orders")

    def test_cancellation_during_sync_keeps_other_orders(self):
        shop = Shop(orders=12)
        third = shop.newest_first()[2]
        _, synced = self.sync(shop, after_call=1, action=lambda: shop.cancel(third))
        self.assertSameOrders(
            shop, synced, "an order that was never cancelled is missing from the nightly export"
        )


if __name__ == "__main__":
    unittest.main()
