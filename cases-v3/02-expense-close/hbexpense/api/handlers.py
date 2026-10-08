"""Endpoint handlers. Each takes (app, request, **path_params) and returns a Response."""
from hbexpense.api.http import Response, month_param, str_param
from hbexpense.util.money import format_krw
from hbexpense.util.text import pad_display


def submit_claim(app, request):
    claim = app.services["claims"].submit(request.body or {})
    return Response(201, claim.to_dict())


def approve_claim(app, request, claim_id):
    return Response.ok(app.services["claims"].approve(claim_id).to_dict())


def reject_claim(app, request, claim_id):
    return Response.ok(app.services["claims"].reject(claim_id).to_dict())


def get_claim(app, request, claim_id):
    return Response.ok(app.repo.get(claim_id).to_dict())


def list_claims(app, request):
    status = str_param(request.query, "status")
    claims = app.repo.all()
    if status:
        claims = [c for c in claims if c.status == status]
    return Response.ok({"items": [c.to_dict() for c in claims]})


def run_settlement(app, request):
    month = month_param(request.query)
    return Response.ok(app.services["settlement"].run(month))


def monthly_report(app, request):
    month = month_param(request.query)
    return Response.ok(app.services["reports"].monthly(month))


def monthly_report_text(app, request):
    month = month_param(request.query)
    report = app.services["reports"].monthly(month)
    out = [f"Expense report {month}", "-" * 64]
    for line in report["lines"]:
        out.append(
            f"{line['receipt']}  {line['date']}  {pad_display(line['memo'], 36)}"
            f"{format_krw(line['amount']):>12}"
        )
    out.append("-" * 64)
    out.append(f"{report['entries']} entries{report['total_display']:>{64 - 10}}")
    return Response.ok({"text": "\n".join(out)})
