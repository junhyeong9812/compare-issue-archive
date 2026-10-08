"""HTTP handlers."""
from orderfeed.api.http import Response, int_param, str_param


def create_order(app, request):
    p = request.params
    created_at = p.get("created_at")
    order = app.services["orders"].create(
        customer=str_param(p, "customer"),
        amount=int_param(p, "amount", minimum=1),
        channel=str_param(p, "channel", "web"),
        created_at=int_param(p, "created_at", minimum=0) if created_at not in (None, "") else None,
    )
    return Response.ok(order.to_dict())


def get_order(app, request, order_id):
    return Response.ok(app.services["orders"].get(int(order_id)).to_dict())


def cancel_order(app, request, order_id):
    app.services["orders"].cancel(int(order_id))
    return Response.ok({"id": int(order_id), "cancelled": True})


def list_orders(app, request):
    p = request.params
    limit = int_param(p, "limit", app.settings.default_page_size, 1, app.settings.max_page_size)
    page = app.services["feed"].page(cursor=p.get("cursor") or None, limit=limit)
    return Response.ok(page)


def daily_report(app, request):
    return Response.ok(app.services["reports"].daily())
