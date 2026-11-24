# storefront 11 — my orders

**Status:** Not started

## Goal
List this buyer's orders on `/orders`. This is the first page that calls Ordering, so it adds the Ordering reference and proxy.

## Depends on
- `storefront/01-storefront-shell`
- `ordering/03-list-buyer-orders`

## Scope
- [ ] AppHost: storefront gets `withEnvironment('services__ordering__http__0', ordering.getEndpoint('http'))` + `waitFor(ordering)` (not `withReference`, which Dockerfile containers reject, PROJECT §7).
- [ ] `vite.config`: proxy `/api/ordering` → `services__ordering__http__0`, with the prefix stripped.
- [ ] `/orders`: `GET /orders?buyerId={buyerId}`, one row per order with date, item count and total, newest first, each linking to `/orders/:id`.
- [ ] Empty state when the buyer has no orders.

## Explicit non-goals
- No order detail content (storefront/12). The page only sends `GET` requests to Ordering.
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
4. **Seed orders:** create two orders for `B` through the proxy: `curl -X POST -H 'Content-Type: application/json' -d '{"productId":1}' <web>/api/basket/basket/$B/items`, then `curl -X POST <web>/api/basket/basket/$B/checkout` → 201 `{"orderId":N}`. Repeat once.
5. **List:** `<web>/orders` shows both orders, newest first, matching `curl '<web>/api/ordering/orders?buyerId=<B>'`. Each row links to `/orders/<id>`.
6. **Empty:** after clearing site data, `/orders` shows the empty state.
7. **Env:** storefront → Environment has `services__ordering__http__0`.

## References
