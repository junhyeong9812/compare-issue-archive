"""Shared fixtures for the API tests: a fresh vendor working dir per test."""
import datetime
import json
import os
import shutil
import tempfile

from hbonboard.app import build_app

FIXTURES = os.path.join(os.path.dirname(__file__), "fixtures")
TODAY = datetime.date(2026, 10, 9)


def load_signups(name="signups_2026_10.json"):
    with open(os.path.join(FIXTURES, name), encoding="utf-8") as f:
        return json.load(f)


class VendorHome:
    def __init__(self):
        self.path = tempfile.mkdtemp(prefix="idchk-")

    def cleanup(self):
        shutil.rmtree(self.path, ignore_errors=True)


def app_with_members(home, signups):
    app = build_app(today=TODAY, idchk_home=home.path)
    ids = []
    for payload in signups:
        resp = app.post("/members", payload)
        assert resp.status == 201, resp.body
        ids.append(resp.body["id"])
    return app, ids
