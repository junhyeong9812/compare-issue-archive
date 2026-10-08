"""Endpoint handlers."""
from storefront.api.http import Response
from storefront.models import BadRequest


def _edits(body):
    if not isinstance(body, list) or not body:
        raise BadRequest("body must be a non-empty list of edits")
    return body


def list_stores(app, request):
    return Response.ok({"stores": app.services["catalog"].stores()})


def list_products(app, request, store):
    products = app.services["catalog"].list_products(store)
    return Response.ok({"store": store, "products": [p.to_dict() for p in products]})


def get_product(app, request, store, sku):
    return Response.ok(app.services["catalog"].get_product(store, sku).to_dict())


def apply_changes(app, request, store):
    summary = app.services["changes"].apply(store, _edits(request.body))
    return Response.ok(summary)


def queue_import(app, request, store):
    job = app.services["imports"].queue(store, _edits(request.body))
    return Response(202, {"job": job.id, "state": job.state})


def tick_jobs(app, request):
    return Response.ok({"ran": app.services["imports"].tick()})


def job_status(app, request, job_id):
    job = app.services["imports"].get(int(job_id))
    return Response.ok({"job": job.id, "store": job.store, "state": job.state, "problems": job.problems})
