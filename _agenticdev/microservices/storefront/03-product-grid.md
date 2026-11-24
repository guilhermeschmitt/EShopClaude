# storefront 03 — product grid

**Status:** Not started

## Goal
Show the first page of products on `/` as a grid of cards. This is the first page that calls Catalog, so it adds the Catalog reference and proxy.

## Depends on
- `storefront/01-storefront-shell`
- `catalog/03-list-products`

## Scope
- [ ] AppHost: storefront gets `withEnvironment('services__catalog__http__0', catalog.getEndpoint('http'))` + `waitFor(catalog)` (not `withReference`, which Dockerfile containers reject, PROJECT §7).
- [ ] `vite.config`: proxy `/api/catalog` → `services__catalog__http__0`, with the prefix stripped.
- [ ] `/`: `GET /products?page=0&size=8`, one vue-base card per product (name, description, price, category).

## Explicit non-goals
- No pager, category select, search box or **Add to cart** (storefront/04–07).
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
4. **Proxy:** `curl '<web>/api/catalog/products?page=0&size=8'` → 200 with 8 `items`, `totalItems` about 24 and `totalPages` 3.
5. **Grid:** `<web>/` shows 8 cards in name order, matching the curl response.
6. **Network:** devtools shows one same-origin `/api/catalog/products?page=0&size=8` request.
7. **Trace:** dashboard → Traces has a catalog `GET /products` trace with a Postgres span on `catalogdb`.
8. **Env:** storefront → Environment has `services__catalog__http__0` and no `services__ordering__*`.

## References
