"""Wiring and a small regex router."""
import re

from orderfeed.api import handlers
from orderfeed.api.http import Request, Response
from orderfeed.config import DEFAULTS
from orderfeed.models import InvalidRequest, NotFound, StaleCursor
from orderfeed.services.feed_service import FeedService
from orderfeed.services.order_service import OrderService
from orderfeed.services.report_service import ReportService
from orderfeed.store.order_store import OrderStore
from orderfeed.util.clock import SystemClock

ROUTES = [
    ("POST", r"^/orders$", handlers.create_order),
    ("GET", r"^/orders$", handlers.list_orders),
    ("GET", r"^/orders/(?P<order_id>\d+)$", handlers.get_order),
    ("POST", r"^/orders/(?P<order_id>\d+)/cancel$", handlers.cancel_order),
    ("GET", r"^/reports/daily$", handlers.daily_report),
]


class App:
    def __init__(self, settings, services):
        self.settings = settings
        self.services = services
        self._routes = [(m, re.compile(p), h) for m, p, h in ROUTES]

    def handle(self, request):
        for method, pattern, handler in self._routes:
            match = pattern.match(request.path)
            if not match or method != request.method:
                continue
            try:
                return handler(self, request, **match.groupdict())
            except NotFound as exc:
                return Response.error(404, "not_found", str(exc))
            except StaleCursor as exc:
                return Response.error(409, "stale_cursor", str(exc))
            except InvalidRequest as exc:
                return Response.error(400, "invalid_request", str(exc))
        return Response.error(404, "no_route", f"{request.method} {request.path}")

    def get(self, path, params=None):
        return self.handle(Request("GET", path, dict(params or {})))

    def post(self, path, params=None):
        return self.handle(Request("POST", path, dict(params or {})))


def build_app(clock=None, **overrides):
    settings = DEFAULTS.with_overrides(**overrides)
    clock = clock or SystemClock()
    store = OrderStore()
    services = {
        "orders": OrderService(store, clock),
        "feed": FeedService(store, settings),
        "reports": ReportService(store, settings),
    }
    return App(settings, services)
