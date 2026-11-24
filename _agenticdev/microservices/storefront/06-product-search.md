# storefront 06 — product search

**Status:** Not started

## Goal
Let the buyer search products by name, combined with the category filter.

## Depends on
- `storefront/05-category-filter`
- `catalog/03-list-products`

## Scope
- [ ] Search box that sends `q=` and resets to page 0, combined with the selected category.

## Explicit non-goals
- No search on description. No client-side filtering of products.
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
4. **Search:** typing `shirt` shows only products whose name contains it (any case), matching `curl '<web>/api/catalog/products?q=shirt&page=0&size=100'`.
5. **Combined:** with a category selected and a query typed, the request carries both `category=` and `q=`, and the grid matches the same curl with both parameters.
6. **Empty:** a query with no match shows an empty-state message, not an error.

## References
