# ordering 02 — create order

**Status:** Not started

## Goal
Persist an order sent by Basket at checkout. Ordering trusts the prices it receives and computes the total.

## Depends on
- `ordering/01-order-schema`

## Scope
- [ ] `POST /orders` `{ buyerId, items: [{ productId, productName, unitPrice, quantity }] }`.
- [ ] 400 when the item list is empty or any quantity is ≤ 0.
- [ ] Compute `total = Σ unitPrice × quantity`, set `created_at`, persist; 201 with the `Order`.
- [ ] Request and `Order` DTO records in `api`.
- [ ] Tests: 201 with the computed total; 400 on an empty list; 400 on quantity 0.

## Explicit non-goals
- No query endpoints (ordering/03, ordering/04).
- No calls to Catalog and no re-pricing.
- No status, no checkout logic.

## Definition of Done
- `mvn test` passes in ordering.
- `aspire run`: ordering healthy.
- curl `POST /orders` against the ordering endpoint URL returns 201 with the right total.
- pgAdmin → orderingdb shows the new rows in `orders` and `order_item`.

## References
