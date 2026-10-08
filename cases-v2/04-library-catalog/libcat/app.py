"""Application wiring and a minimal router (no web framework on purpose)."""
import re

from libcat.api import handlers
from libcat.api.http import Request, Response
from libcat.config import DEFAULTS
from libcat.models import Conflict, NotFound
from libcat.services.harvest import HarvestService
from libcat.services.loans import LoanService
from libcat.services.search import SearchService
from libcat.services.stats import StatsService
from libcat.store.repository import Repository
from libcat.util.cache import TTLCache

ROUTES = [
    ("POST", r"^/harvest$", handlers.harvest),
    ("GET", r"^/search$", handlers.search),
    ("GET", r"^/stats/years$", handlers.stats_years),
    ("GET", r"^/stats/subjects$", handlers.stats_subjects),
    ("GET", r"^/items/(?P<record_id>[^/]+)$", handlers.item_detail),
    ("POST", r"^/loans$", handlers.checkout),
    ("POST", r"^/loans/(?P<record_id>[^/]+)/return$", handlers.return_loan),
]


class App:
    def __init__(self, settings, services):
        self.settings = settings
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
                except ValueError as exc:
                    return Response.error(400, str(exc))
        return Response.error(404, f"no route for {request.method} {request.path}")

    def get(self, path, query=None):
        return self.handle(Request("GET", path, dict(query or {}), ""))

    def post(self, path, body="", query=None):
        return self.handle(Request("POST", path, dict(query or {}), body))


def build_app(clock=None, **overrides):
    settings = DEFAULTS.with_overrides(**overrides)
    repo = Repository()
    cache = TTLCache(settings.stats_cache_ttl, clock=clock)
    services = {
        "harvest": HarvestService(repo, on_change=cache.clear),
        "search": SearchService(repo, settings),
        "stats": StatsService(repo, cache),
        "loans": LoanService(repo, settings),
    }
    return App(settings, services)
