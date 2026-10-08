"""Shared fixtures for the API tests: a fresh ledger journal per test."""
import json
import os
import shutil
import tempfile

from hbexpense.app import build_app

FIXTURES = os.path.join(os.path.dirname(__file__), "fixtures")


def load_claims(name="claims_2026_09.json"):
    with open(os.path.join(FIXTURES, name), encoding="utf-8") as f:
        return json.load(f)


class LedgerHome:
    def __init__(self):
        self.path = tempfile.mkdtemp(prefix="ledger-")

    def cleanup(self):
        shutil.rmtree(self.path, ignore_errors=True)


def app_with_claims(home, claims, approve=True):
    app = build_app(ledger_home=home.path)
    ids = []
    for payload in claims:
        resp = app.post("/claims", payload)
        assert resp.status == 201, resp.body
        ids.append(resp.body["id"])
        if approve:
            assert app.post(f"/claims/{ids[-1]}/approve").status == 200
    return app, ids
