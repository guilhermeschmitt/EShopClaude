# storefront 02 — cart badge

**Status:** Not started

## Goal
Show the buyer's basket in the header: a badge with the sum of quantities, loaded from Basket and kept in shared state that later pages refresh. It also adds the shared error display, because this is the first call that can fail. This is the first spec whose pages call a service, so it adds the Basket reference and proxy.

## Depends on
- `storefront/01-storefront-shell`
- `basket/01-read-basket`

## Scope
- [ ] AppHost: keep `basket` in a variable (`const basket = await builder.addDockerfile('basket', …)`), with its chain unchanged. This is the only edit to basket's registration.
- [ ] AppHost: storefront gets `withEnvironment('services__basket__http__0', basket.getEndpoint('http'))` + `waitFor(basket)` (not `withReference`, which Dockerfile containers reject, PROJECT §7).
- [ ] `vite.config`: proxy `/api/basket` → `services__basket__http__0`, with the prefix stripped.
- [ ] Shared basket state: `GET /basket/{buyerId}` on start, with a refresh function for later specs.
- [ ] Header badge = sum of item quantities (hidden or 0 when empty).
- [ ] Shared error display for failed calls.

## Explicit non-goals
- No catalog or ordering references or proxies. No cart page content (storefront/08).
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
4. **Proxy:** `curl <web>/api/basket/basket/$B` → 200 `{"buyerId":"<B>","items":[],"total":0}`.
5. **Badge:** `curl -X POST -H 'Content-Type: application/json' -d '{"productId":1,"quantity":2}' <web>/api/basket/basket/$B/items`, then the same with `-d '{"productId":2}'`. Reload `<web>/`: the badge shows 3.
6. **Network:** devtools → Network shows only same-origin `/api/basket/basket/<B>` requests.
7. **Error:** stop `basket` in the dashboard and reload. The error display shows the failure and the page still renders. Start basket again.
8. **Env:** dashboard → storefront → Environment has `services__basket__http__0` and no `services__catalog__*` or `services__ordering__*`.

## References
