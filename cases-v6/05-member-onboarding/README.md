# hbonboard

Member onboarding service for Hanbit Pay. People sign up in the app, on the
web or through a partner; a new member stays `pending` until the identity
check comes back. The identity-check run sends new members to the identity
vendor through its CLI (`vendor/bin/idchk`, delivered as a binary by the
vendor), reads each member's verdict back and activates or rejects the
member. Members get a text message with the result, and the admin console
shows the onboarding backlog.

```
hbonboard/
  app.py            wiring (build_app) and the tiny router
  api/              request/response helpers and endpoint handlers
  services/         sign-up, identity-check run, notices, admin backlog
  kyc/              vendor request lines, verdict mapping, CLI wrapper
  store/            in-memory member repository
  util/             phone, names, dates, cache
vendor/bin/         vendor identity-check CLI (idchk --help)
tests/              unit tests and end-to-end API tests
```

The CLI keeps its working files under `$IDCHK_HOME` (tests use a temp dir).
Run all tests with `bash test.sh`.
