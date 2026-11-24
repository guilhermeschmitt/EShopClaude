# storefront 08 — cart view

**Status:** Not started

## Goal
Show the basket on `/cart`: its lines and total exactly as Basket returns them.

## Depends on
- `storefront/02-cart-badge`
- `basket/01-read-basket`

## Scope
- [ ] `/cart`: lines (name, unit price, quantity, line total) and the basket `total`, from the shared basket state.
- [ ] Empty state when the basket has no items.

## Explicit non-goals
- No +/-, remove or **Checkout** button (storefront/09–10). The page shows Basket's `total` and does not compute its own.
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
4. **Lines:** fill the basket as in storefront/02 DoD 5. `<web>/cart` shows both lines, and its total equals `total` from `curl <web>/api/basket/basket/$B`.
5. **Empty:** with a fresh `buyerId` (clear site data), `/cart` shows the empty state.
6. **No catalog call:** devtools → Network on `/cart` shows no `/api/catalog` requests.

## References
