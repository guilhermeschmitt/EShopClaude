# storefront 13 — place order

**Status:** Not started

## Goal
Let the buyer place the order from `/checkout` with one call to Basket, then land on the new order. Disabling the button while the call runs and the failure path stay in this spec, because both are checked on that same single call.

## Depends on
- `storefront/10-checkout-review`
- `storefront/12-order-detail`
- `basket/04-checkout`

## Scope
- [ ] **Place order** on `/checkout` → `POST /basket/{buyerId}/checkout`, disabled while the request runs.
- [ ] On 201 `{ orderId }`: refresh the shared basket state (badge 0) and navigate to `/orders/{orderId}`.
- [ ] On failure: show the error, keep the basket and leave the button usable for a retry.

## Explicit non-goals
- No client-side retry, no idempotency key and no direct call to Ordering from the page.
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
4. **Journey:** add two products, one of them twice, then `/cart` → **Checkout** → **Place order**. The page lands on `/orders/<id>` with the same unit prices and total as the review, and the badge shows 0.
5. **One call:** devtools → Network shows a single `POST /api/basket/basket/<B>/checkout` → 201 and no `/api/ordering` POST.
6. **Double-click:** with items in the basket, double-click **Place order**. `curl '<web>/api/ordering/orders?buyerId=<B>'` gains exactly one order.
7. **Failure:** stop `ordering` in the dashboard, add an item and click **Place order**. The error shows, `/cart` still has the item and the badge is unchanged. Start ordering and retry successfully.
8. **Trace:** dashboard → Traces shows one checkout trace: basket `POST /basket/{buyerId}/checkout` → ordering `POST /orders` → Postgres `INSERT` on `orderingdb`.

## References
