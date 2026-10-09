"""Endpoint handlers. Each takes (app, request, **path_params) and returns a Response."""
from hbstock.api.http import Response, bool_param


def create_product(app, request):
    product = app.services["inventory"].create(request.body or {})
    return Response(201, product.to_dict())


def receive_stock(app, request, sku):
    body = request.body or {}
    return Response.ok(app.services["inventory"].receive(sku, body.get("qty")).to_dict())


def count_stock(app, request, sku):
    body = request.body or {}
    return Response.ok(app.services["inventory"].count(sku, body.get("counted")).to_dict())


def get_product(app, request, sku):
    return Response.ok(app.services["listing"].product_view(sku.upper()))


def place_order(app, request):
    return Response(201, app.services["orders"].place(request.body or {}).to_dict())


def ship_order(app, request, order_id):
    return Response.ok(app.services["orders"].ship(order_id).to_dict())


def cancel_order(app, request, order_id):
    return Response.ok(app.services["orders"].cancel(order_id).to_dict())


def run_sync(app, request):
    return Response.ok(app.services["sync"].run(full=bool_param(request.query or {}, "full")))


def market_listing(app, request):
    return Response.ok(app.services["listing"].listing())


def market_drift(app, request):
    return Response.ok(app.services["listing"].drift())


def low_stock(app, request):
    return Response.ok({"items": app.services["alerts"].current()})
