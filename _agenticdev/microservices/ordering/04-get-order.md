# ordering 04 — get order

**Status:** Not started

## Goal
Return one order with its lines, for the order detail page and to confirm what checkout created.

## Depends on
- `ordering/02-create-order`

## Scope
- [ ] `GET /orders/{id}` → `Order = { id, buyerId, createdAt, total, items[] }`, or 404.
- [ ] Tests: found with items, and 404.

## Explicit non-goals
- No update or delete of orders.

## Definition of Done
- `mvn test` passes in ordering.
- `aspire run`: ordering healthy.
- curl against the ordering endpoint URL: `/orders/{id}` for an order created with `POST /orders` matches its lines and total; an unknown id returns 404.

## References
