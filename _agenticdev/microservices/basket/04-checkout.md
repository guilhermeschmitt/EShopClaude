# basket 04 — checkout

**Status:** Not started

## Goal
Turn the basket into an order. Basket sends its lines with the snapshot prices to Ordering and clears the basket only after the order exists. The Basket → Ordering wiring comes with it.
The success path and the failure path stay in one spec: they are the same flow, and "the basket is kept when Ordering fails" can only be checked against a working checkout.

## Depends on
- `basket/03-update-item-quantity`
- `ordering/02-create-order`
- `ordering/04-get-order`

## Scope
- [ ] AppHost: basket references + waits for `ordering`; `application.yml` maps `services__ordering__http__0` to `ordering.url`.
- [ ] `client/OrderingClient` (`RestClient`, base URL `ordering.url`) calling `POST /orders`.
- [ ] `POST /basket/{buyerId}/checkout`: 400 if the basket is empty or missing; POST `{ buyerId, items }` with snapshot prices; delete `basket:{buyerId}` only after the 201; return 201 `{ orderId }`.
- [ ] An Ordering failure propagates as a 5xx and leaves the basket intact.
- [ ] Tests with `MockRestServiceServer`: success clears the basket; Ordering 500 → 5xx and the basket is kept; empty basket → 400.

## Explicit non-goals
- No re-pricing and no Catalog call at checkout.
- No retries, compensation, sagas or events.
- No changes to Ordering.

## Definition of Done
- AppHost build passes.
- `mvn test` passes in basket.
- `aspire run`: every resource healthy.
- curl against the basket endpoint URL: fill a basket, checkout → 201 `{ orderId }`, `GET /basket/{buyerId}` is empty, and `GET /orders/{orderId}` on ordering matches the lines and total.
- Failure check: stop `ordering` in the dashboard, checkout → 5xx, basket unchanged.
- Dashboard → Traces: a single checkout trace basket → ordering → Postgres.

## References
