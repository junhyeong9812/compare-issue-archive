"""Shared fixtures for the API tests: a fresh vendor store per test."""
import json
import os
import shutil
import tempfile

from hbstock.app import build_app

FIXTURES = os.path.join(os.path.dirname(__file__), "fixtures")


def load_catalog(name="catalog_2026_10.json"):
    with open(os.path.join(FIXTURES, name), encoding="utf-8") as f:
        return json.load(f)


class VendorHome:
    def __init__(self):
        self.path = tempfile.mkdtemp(prefix="stockpush-")

    def cleanup(self):
        shutil.rmtree(self.path, ignore_errors=True)


def app_with_catalog(home, catalog):
    app = build_app(stockpush_home=home.path)
    for payload in catalog:
        resp = app.post("/products", payload)
        assert resp.status == 201, resp.body
    return app


def order(app, sku, qty=1, ship=False):
    resp = app.post("/orders", {"sku": sku, "qty": qty})
    assert resp.status == 201, resp.body
    if ship:
        assert app.post(f"/orders/{resp.body['id']}/ship").status == 200
    return resp.body["id"]
