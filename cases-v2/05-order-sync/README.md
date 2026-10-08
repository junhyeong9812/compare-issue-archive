# orderfeed

Order service for the shop back office.

- `POST /orders` registers an order (online checkout or an offline POS import that
  carries its original `created_at`).
- `POST /orders/<id>/cancel` cancels an order.
- `GET /orders` lists active orders, newest first, page by page (`limit`, `cursor`).
- `GET /reports/daily` sums sales per day.

`orderfeed.client.FeedClient` is the client the accounting tool uses to pull the
full list of active orders every night.

```
orderfeed/
  app.py        wiring and router
  api/          request parsing and handlers
  services/     orders, feed (paging), reports
  store/        sqlite-backed order store
  util/         clock, money, page tokens
  client/       accounting sync client
tests/
```

Run `bash test.sh`.
