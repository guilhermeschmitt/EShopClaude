# storefront 10 — checkout review

**Status:** Not started

## Goal
Let the buyer move from the cart to a read-only review of what they are about to order.

## Depends on
- `storefront/08-cart-view`
- `basket/01-read-basket`

## Scope
- [ ] **Checkout** button on `/cart` → `/checkout`, disabled when the basket is empty.
- [ ] `/checkout`: read-only lines and the basket `total`, from `GET /basket/{buyerId}`.

## Explicit non-goals
- No **Place order** button and no `POST /checkout` (storefront/13). No editing on `/checkout`.
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
4. **Disabled:** with an empty basket, **Checkout** on `/cart` is disabled.
5. **Review:** with items in the basket, **Checkout** opens `/checkout`, which shows the same lines and total as `/cart`, with no +/- or remove controls.
6. **Network:** `/checkout` sends only `GET /api/basket/basket/<B>`.

## References
