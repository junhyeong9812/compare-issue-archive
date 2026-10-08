# hbstock

Stock service for the Hanbit Pay point mall. The warehouse keeps the books
(goods received, stock counts), orders reserve and ship stock, and the market
sync pushes how many units of each product we offer to the open market through
the market's stock sync CLI (`vendor/bin/stockpush`, delivered as a binary by
the vendor). The market listing and the drift report are read back from the
market so they match what shoppers see.

```
hbstock/
  app.py            wiring (build_app) and the tiny router
  api/              request/response helpers and endpoint handlers
  services/         inventory, orders, market sync, market listing, alerts
  market/           stock feed lines and the CLI wrapper
  store/            in-memory products, orders and change tracking
  util/             sku codes and quantities, cache
vendor/bin/         vendor stock sync CLI (stockpush --help)
tests/              unit tests and end-to-end API tests
```

The CLI keeps its store under `$STOCKPUSH_HOME` (tests use a temp dir).
Run all tests with `bash test.sh`.
