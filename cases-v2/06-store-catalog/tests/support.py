"""Builds a throw-away data root with a few branches."""
import json
import os
import shutil

from storefront.app import build_app

WORK = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "_work")

SEED = {
    "seoul": [
        ("TEA-01", "Barley Tea", 3000, 40),
        ("COF-02", "Drip Coffee", 4200, 25),
        ("BRD-07", "Sesame Bread", 2800, 12),
        ("SNK-11", "Rice Crackers", 1900, 60),
    ],
    "busan": [
        ("FSH-03", "Fish Cake Box", 12000, 8),
        ("TEA-01", "Barley Tea", 3100, 30),
        ("SNK-20", "Seaweed Chips", 2500, 44),
    ],
    "_shared": [
        ("GFT-10", "Gift Card 10k", 10000, 999),
    ],
}


def make_root(test_case, seed=SEED):
    root = os.path.join(WORK, test_case.id())
    shutil.rmtree(root, ignore_errors=True)
    os.makedirs(root)
    test_case.addCleanup(shutil.rmtree, root, ignore_errors=True)
    for store, products in seed.items():
        directory = os.path.join(root, store, "products")
        os.makedirs(directory)
        for sku, name, price, stock in products:
            data = {"sku": sku, "name": name, "price": price, "stock": stock, "shared": store == "_shared"}
            with open(os.path.join(directory, f"{sku}.json"), "w", encoding="utf-8") as f:
                json.dump(data, f)
    return root


def make_app(test_case, **overrides):
    return build_app(make_root(test_case), **overrides)


def listing(app, store):
    resp = app.get(f"/stores/{store}/products")
    assert resp.status == 200, resp.body
    return {p["sku"]: (p["name"], p["price"], p["stock"]) for p in resp.body["products"]}
