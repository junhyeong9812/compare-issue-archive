"""Endpoint handlers: translate HTTP-ish requests into service calls."""
from libcat.api.http import Response, int_param, str_param


def harvest(app, request):
    report = app.services["harvest"].ingest(request.body)
    return Response.ok(report)


def search(app, request):
    q = request.query
    page_size = int_param(q, "size", app.settings.search_page_size, 1, app.settings.max_page_size)
    page = int_param(q, "page", 1, minimum=1)
    year = int_param(q, "year")
    year_from = int_param(q, "from", year)
    year_to = int_param(q, "to", year)
    result = app.services["search"].search(
        text=str_param(q, "q"),
        subject=str_param(q, "subject"),
        year_from=year_from,
        year_to=year_to,
        page=page,
        page_size=page_size,
    )
    return Response.ok(result)


def stats_years(app, request):
    return Response.ok(app.services["stats"].years())


def stats_subjects(app, request):
    return Response.ok(app.services["stats"].subjects())


def item_detail(app, request, record_id):
    record = app.services["search"].get(record_id)
    return Response.ok(record.to_dict())


def checkout(app, request):
    q = request.query
    loan = app.services["loans"].checkout(
        str_param(q, "record"), str_param(q, "member"), int_param(q, "day", 0, minimum=0)
    )
    return Response.ok({"record": loan.record_id, "member": loan.member_id, "due_day": loan.due_day})


def return_loan(app, request, record_id):
    late = app.services["loans"].give_back(record_id, int_param(request.query, "day", 0, minimum=0))
    return Response.ok({"record": record_id, "late_days": late})
