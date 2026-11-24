# storefront 05 — category filter

**Status:** Not started

## Goal
Let the buyer narrow the product grid to one category.

## Depends on
- `storefront/04-product-pager`
- `catalog/05-list-categories`

## Scope
- [ ] Category select filled from `GET /categories`, with an "All" option.
- [ ] Choosing a category sends `category=` and resets to page 0. The pager follows the filtered `totalPages`.

## Explicit non-goals
- No search box (storefront/06). No client-side filtering of products.
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
4. **Options:** the select lists exactly the result of `curl <web>/api/catalog/categories`, plus "All".
5. **Filter:** choosing a category shows only that category's cards, and their count matches `totalItems` from `curl '<web>/api/catalog/products?category=<c>&page=0&size=100'`.
6. **Reset:** on page 2, choosing a category returns to page 1. Choosing "All" shows every product again.

## References
