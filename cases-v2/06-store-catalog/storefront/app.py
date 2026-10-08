"""Wiring and router."""
import re

from storefront.api import handlers
from storefront.api.http import Request, Response
from storefront.config import Settings
from storefront.models import BadRequest, NotFound, Rejected
from storefront.services.catalog_service import CatalogService
from storefront.services.change_service import ChangeService
from storefront.services.import_jobs import ImportJobs
from storefront.store.product_repo import ProductRepo

ROUTES = [
    ("GET", r"^/stores$", handlers.list_stores),
    ("GET", r"^/stores/(?P<store>[a-z0-9_-]+)/products$", handlers.list_products),
    ("GET", r"^/stores/(?P<store>[a-z0-9_-]+)/products/(?P<sku>[A-Z0-9-]+)$", handlers.get_product),
    ("POST", r"^/stores/(?P<store>[a-z0-9_-]+)/changes$", handlers.apply_changes),
    ("POST", r"^/stores/(?P<store>[a-z0-9_-]+)/imports$", handlers.queue_import),
    ("POST", r"^/jobs/tick$", handlers.tick_jobs),
    ("GET", r"^/jobs/(?P<job_id>\d+)$", handlers.job_status),
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
                return Response.error(404, str(exc))
            except Rejected as exc:
                return Response(422, {"error": "rejected", "problems": exc.problems})
            except BadRequest as exc:
                return Response.error(400, str(exc))
        return Response.error(404, f"no route for {request.method} {request.path}")

    def get(self, path):
        return self.handle(Request("GET", path))

    def post(self, path, body=None):
        return self.handle(Request("POST", path, body))


def build_app(data_root, **overrides):
    settings = Settings.for_root(data_root, **overrides)
    repo = ProductRepo(settings.data_root)
    changes = ChangeService(repo, settings)
    services = {
        "catalog": CatalogService(repo, settings),
        "changes": changes,
        "imports": ImportJobs(changes, settings),
    }
    return App(settings, services)
