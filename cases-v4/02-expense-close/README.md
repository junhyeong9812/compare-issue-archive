# hbexpense

Expense settlement service for Hanbit Pay staff. Employees submit expense
claims, team leads approve them, and the month-end settlement books every
approved claim into the company ledger through the ledger vendor's CLI
(`vendor/bin/ledgerctl`, delivered as a binary by the vendor). The month-end
report is read back from the ledger so it matches what finance sees.

```
hbexpense/
  app.py            wiring (build_app) and the tiny router
  api/              request/response helpers and endpoint handlers
  services/         claims workflow, settlement run, monthly report
  ledger/           ledger entry mapping and the CLI wrapper
  store/            in-memory claim repository
  util/             text, dates, money, cache
vendor/bin/         vendor ledger CLI (ledgerctl --help)
tests/              unit tests and end-to-end API tests
```

The CLI keeps its journal under `$LEDGERCTL_HOME` (tests use a temp dir).
Run all tests with `bash test.sh`.
