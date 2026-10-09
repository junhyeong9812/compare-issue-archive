"""Application wiring and a minimal router (no web framework on purpose)."""
import datetime as _dt
import re

from hbonboard.api import handlers
from hbonboard.api.http import Request, Response
from hbonboard.config import DEFAULTS
from hbonboard.kyc.client import IdchkClient
from hbonboard.models import Conflict, NotFound, VendorError
from hbonboard.services.backlog import BacklogReport
from hbonboard.services.notify import Outbox
from hbonboard.services.signup import SignupService
from hbonboard.services.verification import VerificationService
from hbonboard.store.repository import MemberRepository
from hbonboard.util.cache import TTLCache

ROUTES = [
    ("POST", r"^/members$", handlers.register_member),
    ("GET", r"^/members$", handlers.list_members),
    ("GET", r"^/members/(?P<member_id>[^/]+)$", handlers.get_member),
    ("POST", r"^/kyc/run$", handlers.run_verification),
    ("GET", r"^/reports/kyc-backlog$", handlers.kyc_backlog),
    ("GET", r"^/outbox$", handlers.outbox),
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


def build_app(today=None, clock=None, **overrides):
    settings = DEFAULTS.with_overrides(**overrides)
    today = today or _dt.date.today()
    repo = MemberRepository()
    outbox = Outbox()
    backlog = BacklogReport(repo, settings, TTLCache(settings.report_cache_ttl, clock=clock))
    services = {
        "signup": SignupService(repo, settings, today),
        "backlog": backlog,
        "outbox": outbox,
        "verification": VerificationService(
            repo, IdchkClient(settings), settings, today,
            notifier=outbox, on_resolved=backlog.invalidate,
        ),
    }
    return App(settings, repo, services)
