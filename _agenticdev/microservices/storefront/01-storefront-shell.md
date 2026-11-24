# storefront 01 — storefront shell

**Status:** Not started

## Goal
Create the storefront project and register it in the AppHost, with the app shell every page lives in: the header, the placeholder routes and the anonymous `buyerId`. Later specs only add pages and calls.

## Depends on
- nothing

## Scope
- [ ] `src/vue-front/storefront/`: Vue 3 + Vite + vue-base, `Core.init(app, router, routes, { baseURL: '/api' })`, every route public (no auth guards).
- [ ] Header: app name → `/`, **My orders** → `/orders`, cart icon → `/cart` (no badge yet).
- [ ] Placeholder pages for `/`, `/cart`, `/checkout`, `/orders`, `/orders/:id`.
- [ ] `buyerId`: UUID v4 generated on first load and kept in `localStorage['eshop.buyerId']`.
- [ ] AppHost: `storefront` through `addViteApp('storefront', './vue-front/storefront')` with external HTTP endpoints, and **no** references, waits or proxy entries.

## Explicit non-goals
- No service calls, no `vite.config` proxy entries and no cart badge (storefront/02).
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
4. **Home:** open `<web>/`. The header and the home placeholder render, and the browser console has no errors.
5. **Navigation:** clicking the app name, **My orders** and the cart icon goes to `/`, `/orders` and `/cart`. Opening `<web>/checkout` and `<web>/orders/1` directly shows their placeholders.
6. **buyerId:** devtools → Local Storage holds `eshop.buyerId` as a UUID v4. After a reload the value is unchanged. After clearing site data and reloading, a new UUID appears.
7. **No references:** dashboard → storefront → Environment has no `services__*` variables.

## References
