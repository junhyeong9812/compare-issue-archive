"""Application wiring and a minimal router (no web framework on purpose)."""
import re

from hbstock.api import handlers
from hbstock.api.http import Request, Response
from hbstock.config import DEFAULTS
from hbstock.market.client import StockpushClient
from hbstock.models import Conflict, NotFound, VendorError
from hbstock.services.alerts import LowStockAlerts
from hbstock.services.inventory import InventoryService
from hbstock.services.listing import ListingService
from hbstock.services.orders import OrderService
from hbstock.services.sync import SyncService
from hbstock.store.repository import StockRepository
from hbstock.util.cache import TTLCache

ROUTES = [
    ("POST", r"^/products$", handlers.create_product),
    ("GET", r"^/products/(?P<sku>[^/]+)$", handlers.get_product),
    ("POST", r"^/products/(?P<sku>[^/]+)/receive$", handlers.receive_stock),
    ("POST", r"^/products/(?P<sku>[^/]+)/count$", handlers.count_stock),
    ("POST", r"^/orders$", handlers.place_order),
    ("POST", r"^/orders/(?P<order_id>[^/]+)/ship$", handlers.ship_order),
    ("POST", r"^/orders/(?P<order_id>[^/]+)/cancel$", handlers.cancel_order),
    ("POST", r"^/market/sync$", handlers.run_sync),
    ("GET", r"^/market/listing$", handlers.market_listing),
    ("GET", r"^/reports/market-drift$", handlers.market_drift),
    ("GET", r"^/reports/low-stock$", handlers.low_stock),
]


class App:
    def __init__(self, settings, repo, services):
        self.settings = settings
        self.repo = repo
        self.services = services
        self._routes = [(m, re.compile(p), h) for m, p, h in ROUTES]

    def handle(self, request):
        for method, pattern, handler in self._routes:
            match = pattern.match(request.path)
            if match and method == request.method:
                try:
                    return handler(self, request, **match.groupdict())
                except NotFound as exc:
                    return Response.error(404, str(exc))
                except Conflict as exc:
                    return Response.error(409, str(exc))
                except VendorError as exc:
                    return Response.error(502, str(exc))
                except ValueError as exc:
                    return Response.error(400, str(exc))
        return Response.error(404, f"no route for {request.method} {request.path}")

    def get(self, path, query=None):
        return self.handle(Request("GET", path, dict(query or {}), None))

    def post(self, path, body=None, query=None):
        return self.handle(Request("POST", path, dict(query or {}), body))


def build_app(clock=None, **overrides):
    settings = DEFAULTS.with_overrides(**overrides)
    repo = StockRepository()
    client = StockpushClient(settings)
    listing = ListingService(repo, client, settings, TTLCache(settings.listing_cache_ttl, clock=clock))
    services = {
        "inventory": InventoryService(repo, settings),
        "orders": OrderService(repo),
        "alerts": LowStockAlerts(repo, settings),
        "listing": listing,
        "sync": SyncService(repo, client, settings, on_synced=listing.invalidate),
    }
    return App(settings, repo, services)
