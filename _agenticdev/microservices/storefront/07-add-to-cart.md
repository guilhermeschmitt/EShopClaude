# storefront 07 — add to cart

**Status:** Not started

## Goal
Let the buyer add a product from its card. Basket looks the price up itself, so the page sends only the product id.

## Depends on
- `storefront/02-cart-badge`
- `storefront/03-product-grid`
- `basket/02-add-item`

## Scope
- [ ] **Add to cart** on each card → `POST /basket/{buyerId}/items { productId, quantity: 1 }`.
- [ ] On success the shared basket state refreshes, so the badge updates. On failure the shared error display shows it.

## Explicit non-goals
- No quantity picker on the card. No AppHost change (the basket reference came with storefront/02).
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
4. **Add:** with an empty basket, click **Add to cart** on two products, and twice on one of them. The badge shows 3.
5. **Request body:** devtools → Network shows `POST /api/basket/basket/<B>/items` with body `{"productId":<id>,"quantity":1}` and no price field.
6. **Snapshot:** `curl <web>/api/basket/basket/$B` shows two lines with quantities 2 and 1, whose `unitPrice` equals the prices on the cards.
7. **Trace:** dashboard → Traces shows basket `POST /basket/{buyerId}/items` → catalog `GET /products/{id}` → Postgres.

## References
