# ordering 03 — list buyer orders

**Status:** Not started

## Goal
List a buyer's orders as summaries, newest first, for the storefront's "My orders" page.

## Depends on
- `ordering/02-create-order`

## Scope
- [ ] `GET /orders?buyerId={uuid}` → `OrderSummary[] = { id, createdAt, total, itemCount }`, newest first, not paged.
- [ ] Tests: newest-first ordering, `itemCount`, an unknown buyer returns `[]`.

## Explicit non-goals
- No paging and no filters other than `buyerId`.
- No order detail (ordering/04).

## Definition of Done
- `mvn test` passes in ordering.
- `aspire run`: ordering healthy.
- curl against the ordering endpoint URL: after creating two orders for one buyer, `/orders?buyerId=<uuid>` lists both, newest first.
- Dashboard → Traces: ordering → Postgres span.

## References
