# libcat

Small catalog service for the branch libraries. It ingests publisher feeds,
keeps the catalog in memory, and serves search, statistics and loan endpoints.

```
libcat/
  app.py            wiring (build_app) and the tiny router
  api/              request/response helpers and endpoint handlers
  services/         harvest, search, stats, loans
  store/            repository and secondary indexes
  util/             feed format, dates, text folding, cache
tests/              unit tests and end-to-end API tests
```

Run all tests with `bash test.sh`.
