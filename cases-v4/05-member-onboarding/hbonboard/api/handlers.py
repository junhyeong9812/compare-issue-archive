"""Endpoint handlers. Each takes (app, request, **path_params) and returns a Response."""
from hbonboard.api.http import Response, str_param
from hbonboard.util.phone import mask_phone


def register_member(app, request):
    member = app.services["signup"].register(request.body or {})
    app.services["backlog"].invalidate()
    return Response(201, member.to_dict(mask=mask_phone))


def get_member(app, request, member_id):
    return Response.ok(app.repo.get(member_id).to_dict(mask=mask_phone))


def list_members(app, request):
    status = str_param(app_query(request), "status")
    members = sorted(app.repo.all(), key=lambda m: m.member_id)
    if status:
        members = [m for m in members if m.status == status]
    return Response.ok({"items": [m.to_dict(mask=mask_phone) for m in members]})


def run_verification(app, request):
    return Response.ok(app.services["verification"].run())


def kyc_backlog(app, request):
    return Response.ok(app.services["backlog"].summary())


def outbox(app, request):
    member_id = str_param(app_query(request), "member")
    return Response.ok({"items": app.services["outbox"].messages(member_id)})


def app_query(request):
    return request.query or {}
