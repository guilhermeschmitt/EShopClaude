# storefront 09 — cart adjust

**Status:** Not started

## Goal
Let the buyer change quantities and remove lines on `/cart`. Remove stays in this spec because it is the same `PUT` with `quantity: 0`, so it can't be verified apart from +/-.

## Depends on
- `storefront/08-cart-view`
- `basket/03-update-item-quantity`

## Scope
- [ ] +/- on each line → `PUT /basket/{buyerId}/items/{productId} { quantity }`. Pressing - at quantity 1 removes the line.
- [ ] Remove on each line → the same `PUT` with `quantity: 0`.
- [ ] After each call the shared basket state refreshes (lines, total, badge).

## Explicit non-goals
- No free-text quantity input. No optimistic update that shows numbers Basket didn't return.
- Prices come only from what Basket and Ordering return. No page asks Catalog for a price again, and no request body sends a price to any service (adding sends only `productId` and `quantity`).
- Checkout is one call to Basket (`POST /basket/{buyerId}/checkout`). No page calls Ordering to create anything (no `POST /orders`).
- No page reads a database. The browser talks only to the storefront origin, through `/api/{service}`.
- No changes to catalog, basket, ordering, postgres, redis or their AppHost registrations.
- No AppHost reference to a service this spec's pages don't call.
- Nothing from PROJECT.md §9: no login or auth, payments, shipping, product admin or images, stock, order status, checkout form, re-pricing, retries or fallbacks, order paging, or frontend tests.

## Definition of Done
`<web>` is the storefront URL from the dashboard. `B` is `localStorage['eshop.buyerId']`, read in the browser devtools; in Git Bash, `B=<that value>`.

1. **AppHost build:** in `src/`, `npm run build` exits 0.
2. **Storefront build:** in `src/vue-front/storefront`, `npm run build` exits 0.
3. **Healthy start:** in `src/`, `aspire run` shows every resource (postgres, catalogdb, orderingdb, pgAdmin, redis, catalog, basket, ordering, storefront) Running/Healthy.
4. **Change:** on `/cart`, pressing + on a line raises its quantity by 1. The line total, cart total and badge match `curl <web>/api/basket/basket/$B`.
5. **Remove:** remove a line. It disappears, and the total and badge drop to match the curl.
6. **Request body:** devtools shows `PUT /api/basket/basket/<B>/items/<id>` with body `{"quantity":N}` only.

## References
