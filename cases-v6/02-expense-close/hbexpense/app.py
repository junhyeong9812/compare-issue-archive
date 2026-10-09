"""Application wiring and a minimal router (no web framework on purpose)."""
import re

from hbexpense.api import handlers
from hbexpense.api.http import Request, Response
from hbexpense.config import DEFAULTS
from hbexpense.ledger.client import LedgerClient
from hbexpense.models import Conflict, LedgerError, NotFound
from hbexpense.services.claims import ClaimService
from hbexpense.services.reports import ReportService
from hbexpense.services.settlement import SettlementService
from hbexpense.store.repository import ClaimRepository
from hbexpense.util.cache import TTLCache

ROUTES = [
    ("POST", r"^/claims$", handlers.submit_claim),
    ("GET", r"^/claims$", handlers.list_claims),
    ("GET", r"^/claims/(?P<claim_id>[^/]+)$", handlers.get_claim),
    ("POST", r"^/claims/(?P<claim_id>[^/]+)/approve$", handlers.approve_claim),
    ("POST", r"^/claims/(?P<claim_id>[^/]+)/reject$", handlers.reject_claim),
    ("POST", r"^/settlement/run$", handlers.run_settlement),
    ("GET", r"^/reports/monthly$", handlers.monthly_report),
    ("GET", r"^/reports/monthly\.txt$", handlers.monthly_report_text),
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
                except LedgerError as exc:
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
    repo = ClaimRepository()
    ledger = LedgerClient(settings)
    reports = ReportService(repo, ledger, TTLCache(settings.report_cache_ttl, clock=clock))
    services = {
        "claims": ClaimService(repo, settings),
        "reports": reports,
        "settlement": SettlementService(repo, ledger, settings, on_posted=reports.invalidate),
    }
    return App(settings, repo, services)
