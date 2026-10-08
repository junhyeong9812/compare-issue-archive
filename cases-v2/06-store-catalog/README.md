# storefront

Back-office catalog service for the shop branches. Each branch ("store") has its
own product catalog on disk; a shared catalog (gift cards and the like) is
merged into every branch listing.

- `GET  /stores/<store>/products` / `GET /stores/<store>/products/<sku>`
- `POST /stores/<store>/changes` applies a list of edits as one revision:
  either every edit is published or none is.
- `POST /stores/<store>/imports` queues a large edit list as a background job;
  the worker (`POST /jobs/tick`) processes a few edits per tick.

```
storefront/
  app.py          wiring and router
  api/            request objects and handlers
  services/       catalog, changes, import jobs, validation
  store/          product files, revisions
  util/           filesystem helpers, sku rules
tests/
```

Run `bash test.sh`.
