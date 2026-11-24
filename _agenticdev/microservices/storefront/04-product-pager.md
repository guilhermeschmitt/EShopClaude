# storefront 04 — product pager

**Status:** Not started

## Goal
Let the buyer page through all ~24 products on `/`.

## Depends on
- `storefront/03-product-grid`
- `catalog/03-list-products`

## Scope
- [ ] Pager under the grid, driven by `page` and the response's `totalPages`, with `size=8`.

## Explicit non-goals
- No category select or search (storefront/05–06).
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
4. **Paging:** on `<web>/`, pages 2 and 3 each show 8 different cards. The three pages together list every product once (compare with `curl '<web>/api/catalog/products?page=0&size=100'`).
5. **Network:** each page change sends `/api/catalog/products?page=N&size=8`.

## References
