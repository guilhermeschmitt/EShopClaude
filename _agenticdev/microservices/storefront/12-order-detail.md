# storefront 12 — order detail

**Status:** Not started

## Goal
Show one order on `/orders/:id`: its lines with the snapshot prices, the total and the date.

## Depends on
- `storefront/11-my-orders`
- `ordering/04-get-order`

## Scope
- [ ] `/orders/:id`: `GET /orders/{id}`, lines (name, unit price, quantity, line total), total and date.
- [ ] A not-found message on 404.

## Explicit non-goals
- No reorder, cancel or status. The page shows Ordering's prices and total and never asks Catalog.
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
4. **Detail:** from `/orders`, open an order created in storefront/11 DoD 4. Its lines, total and date match `curl <web>/api/ordering/orders/<id>`.
5. **Not found:** `<web>/orders/999999` shows the not-found message.
6. **Network:** the page sends only `GET /api/ordering/orders/<id>`.

## References
