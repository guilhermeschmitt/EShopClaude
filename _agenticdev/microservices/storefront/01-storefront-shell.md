# storefront 01 — storefront shell

**Status:** Not started

## Goal
Create the storefront project and register it in the AppHost, with the app shell every page lives in: the header, the placeholder routes and the anonymous `buyerId`. Later specs only add pages and calls.

## Context
- This is the first storefront spec and the first front-end code in the repository. `src/vue-front/` doesn't exist yet. The starter's `src/frontend` was deleted in PLAN 1.1, and the storefront was never built in Phase 1 (PLAN Phase 5, "Where the old steps went").
- `src/apphost.mts` today registers `postgres` (+ pgAdmin), `catalogdb`, `orderingdb`, `redis`, and `catalog`, `ordering` and `basket` through `addDockerfile`. It has no JavaScript resource.
- `Aspire.Hosting.JavaScript` 17.0.0 is already in `aspire.config.json`. Its generated API in `src/.aspire/modules/aspire.mts` provides `addViteApp(name, appDirectory, options?)` (run script defaults to `dev`) and, on `ViteAppResource`, `withNpm({ install })`, `withExternalHttpEndpoints()`, `withHttpHealthCheck({ path })` and `withViteConfig(path)`. Use those names; don't take them from the C# docs.
- vue-base is the library at `github.com/guilhermeschmitt/vue-base`, consumed as an npm git dependency pinned to tag `v0.0.1`. It provides `Core.init(app, router, routes, options)`, its own UI components and a bundled axios (PROJECT.md §6). Its package name, exports, route-guard convention and icon support aren't written in any project document. Read them from `node_modules/vue-base` (`package.json`, entry file, README) after `npm install`, before writing code that uses them.
- The storefront isn't instrumented with OpenTelemetry (PROJECT.md §2), so there's no `withOtlpExporter()` and no cert-trust env var on its registration.
- No Java service changes, so there's no `mvn test` for this spec. CLAUDE.md requires it only for Java services you touched.

## Depends on
Nothing.

## Scope
- [ ] `src/vue-front/storefront/`: Vue 3 + Vite + TypeScript + vue-base (npm git dependency, pinned to tag `v0.0.1`), `Core.init(app, router, routes, { baseURL: '/api' })`, every route public (no auth guards).
- [ ] Header: app name → `/`, **My orders** → `/orders`, cart icon → `/cart` (no badge yet).
- [ ] Placeholder pages for `/`, `/cart`, `/checkout`, `/orders`, `/orders/:id`.
- [ ] `buyerId`: UUID v4 generated on first load and kept in `localStorage['eshop.buyerId']`.
- [ ] AppHost: `storefront` through `addViteApp('storefront', './vue-front/storefront')` with `withNpm({ install: true })`, external HTTP endpoints and an HTTP health check on `/`, and **no** references, waits or proxy entries.

## Explicit non-goals
- No service calls, no `vite.config` proxy entries and no cart badge (storefront/02).
- Prices come only from what Basket and Ordering return. No page asks Catalog for a price again, and no request body sends a price to any service (adding sends only `productId` and `quantity`).
- Checkout is one call to Basket (`POST /basket/{buyerId}/checkout`). No page calls Ordering to create anything (no `POST /orders`).
- No page reads a database. The browser talks only to the storefront origin, through `/api/{service}`.
- No changes to catalog, basket, ordering, postgres, redis or their AppHost registrations.
- No AppHost reference to a service this spec's pages don't call.
- Nothing from PROJECT.md §9: no login or auth, payments, shipping, product admin or images, stock, order status, checkout form, re-pricing, retries or fallbacks, order paging, or frontend tests.
- No `withOtlpExporter()` or other OpenTelemetry setup on the storefront (PROJECT.md §2).
- No state store (Pinia or similar) and no API client module yet. The first spec that calls a service adds what it needs.

## Data model
None on the server. The only state is in the browser:

| Key | Storage | Value | Written |
|---|---|---|---|
| `eshop.buyerId` | `localStorage` | UUID v4 string, lower case, from `crypto.randomUUID()` | Once, on first load, when the key is missing or empty |

- `crypto.randomUUID()` needs a secure context. `http://localhost` counts as one, and that's how the storefront is served under Aspire. No `uuid` package.
- **Module** `src/buyer.ts` exports `getBuyerId(): string`. It reads the key and, if it's missing, generates the value, stores it and returns it. `main.ts` calls it once before mounting, so the id exists on the first load of any route. Later specs import `getBuyerId()` and never touch `localStorage` directly.

**Routes** (`src/router/routes.ts`), all public:

| Path | Name | Component |
|---|---|---|
| `/` | `home` | `views/HomeView.vue` |
| `/cart` | `cart` | `views/CartView.vue` |
| `/checkout` | `checkout` | `views/CheckoutView.vue` |
| `/orders` | `orders` | `views/OrdersView.vue` |
| `/orders/:id` | `order-detail` | `views/OrderDetailView.vue` |

## Tasks
1. **Project scaffold** (`src/vue-front/storefront/`), following the create-vue TypeScript layout:
   - `package.json`: `"private": true`, `"type": "module"`, and the scripts `dev` = `vite`, `build` = `vue-tsc -b && vite build`, `preview` = `vite preview`. Dependencies: `vue`, `vue-router` and `"vue-base": "github:guilhermeschmitt/vue-base#v0.0.1"`. Dev dependencies: `vite`, `@vitejs/plugin-vue`, `typescript`, `vue-tsc`, `@vue/tsconfig` and `@types/node`. Don't add axios; vue-base bundles it. Run `npm install` and commit `package-lock.json`.
   - `index.html` with `<div id="app">` and `<script type="module" src="/src/main.ts">`, title `EShop`.
   - `tsconfig.json`, `tsconfig.app.json`, `tsconfig.node.json` and `env.d.ts` as create-vue generates them.
   - `vite.config.ts`: `@vitejs/plugin-vue` only, plus the `@` → `src` alias. There's no `server.proxy` and no hard-coded `server.port`; Aspire assigns the port.
   - `.gitignore`: `node_modules/` and `dist/`.
2. **vue-base:** after `npm install`, read `node_modules/vue-base` to confirm the import name of `Core`, the shape of `Core.init`'s options, how a route is marked public (meta flag or guard option) and which header/icon components it exports. Follow what's there, and record any difference from this spec in the plan.
3. **Bootstrap** `src/main.ts`: `getBuyerId()`, then `createApp(App)`, `createRouter({ history: createWebHistory(), routes })` and `Core.init(app, router, routes, { baseURL: '/api' })`, mounted on `#app`. Every route is public by vue-base's convention, so no guard redirects.
4. **Shell** `src/App.vue`: `<AppHeader />` + `<RouterView />`.
5. **Header** `src/components/AppHeader.vue`, built from vue-base components:
   - the app name `EShop` as a `RouterLink` to `/`;
   - **My orders**, a `RouterLink` to `/orders`;
   - a cart icon `RouterLink` to `/cart` with `aria-label="Cart"`, using vue-base's icon if it has one, otherwise an inline SVG. No badge.
6. **Placeholder views** under `src/views/`: each renders an `<h1>` (`Products`, `Cart`, `Checkout`, `My orders`, `Order #{id}`). `OrderDetailView` reads `id` from the route params. No service calls.
7. **buyerId** `src/buyer.ts`, as described in Data model.
8. **AppHost** (`src/apphost.mts`), after the `basket` block and before `builder.build().run()`:
   ```ts
   // Storefront: Vue 3 + Vite dev server. No service references yet (the first page that calls a service adds them).
   await builder
       .addViteApp('storefront', './vue-front/storefront')
       .withNpm({ install: true })
       .withExternalHttpEndpoints()
       .withHttpHealthCheck({ path: '/' });
   ```
   Every builder call is awaited (`no-floating-promises`). No other registration changes.

## Files touched
- `src/apphost.mts` (edit)
- `src/vue-front/storefront/package.json` (new)
- `src/vue-front/storefront/package-lock.json` (new)
- `src/vue-front/storefront/.gitignore` (new)
- `src/vue-front/storefront/index.html` (new)
- `src/vue-front/storefront/vite.config.ts` (new)
- `src/vue-front/storefront/tsconfig.json`, `tsconfig.app.json`, `tsconfig.node.json`, `env.d.ts` (new)
- `src/vue-front/storefront/src/main.ts` (new)
- `src/vue-front/storefront/src/App.vue` (new)
- `src/vue-front/storefront/src/buyer.ts` (new)
- `src/vue-front/storefront/src/router/routes.ts` (new)
- `src/vue-front/storefront/src/components/AppHeader.vue` (new)
- `src/vue-front/storefront/src/views/HomeView.vue`, `CartView.vue`, `CheckoutView.vue`, `OrdersView.vue`, `OrderDetailView.vue` (new)

Nothing in catalog, basket or ordering changes, and neither do their AppHost registrations.

## Definition of Done
Run every check from the repository root unless it says otherwise. `<web>` is the storefront URL from the dashboard (storefront → Endpoints). Browser checks can be run by hand or with the Playwright MCP (snapshot + console messages).

1. **AppHost build:** in `src/`, `npm run build` exits 0 with no eslint or tsc errors.
2. **Storefront build:** in `src/vue-front/storefront`, `npm ci && npm run build` exits 0, `vue-tsc` reports no errors, and `dist/index.html` exists.
3. **vue-base pinned:** `grep '"vue-base"' src/vue-front/storefront/package.json` shows `github:guilhermeschmitt/vue-base#v0.0.1`, and `package-lock.json` has a `node_modules/vue-base` entry whose `resolved` URL ends in a commit SHA.
4. **Healthy start:** in `src/`, `aspire run` shows every resource (postgres, catalogdb, orderingdb, pgAdmin, redis, catalog, basket, ordering, storefront) as Running/Healthy. On a fresh clone (no `node_modules`), storefront's console logs show `npm install` running before Vite starts.
5. **Health endpoint:** `curl -s -o /dev/null -w '%{http_code}' <web>/` prints `200`.
6. **SPA fallback:** `curl -s -o /dev/null -w '%{http_code}' <web>/orders/1` prints `200`.
7. **Home:** open `<web>/`. The header (`EShop`, **My orders**, cart icon) and the `Products` heading render, and the browser console has no errors or warnings from the app.
8. **Navigation:** clicking `EShop`, **My orders** and the cart icon goes to `/`, `/orders` and `/cart` (headings `Products`, `My orders`, `Cart`) without a full page reload. Opening `<web>/checkout` and `<web>/orders/1` directly shows `Checkout` and `Order #1`. None of these redirect to a login route.
9. **buyerId:** in the devtools console, `localStorage.getItem('eshop.buyerId')` matches `/^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/`. After a reload the value is unchanged. After `localStorage.clear()` and a reload, a different value that also matches appears. Opening `<web>/orders/1` first in a fresh profile also creates it.
10. **No service calls:** devtools → Network on `/`, `/cart`, `/checkout`, `/orders` and `/orders/1` shows no `/api/` requests.
11. **No references:** dashboard → storefront → Environment has no `services__*` variables, and `grep -n proxy src/vue-front/storefront/vite.config.ts` prints nothing.
12. **Scope fence:** `git diff --stat main...HEAD` lists only `src/apphost.mts`, files under `src/vue-front/storefront/` and files under `_agenticdev/`. `git diff main...HEAD -- src/apphost.mts` shows only the added storefront block.

## References
- PROJECT.md §2 Architecture (layout `src/vue-front/storefront/`, the browser talks only to the storefront origin, storefront not instrumented), §3 Identity without login (`buyerId`, `eshop.buyerId`), §6 Storefront (`Core.init`, public routes, header, route table), §7 Aspire AppHost (`storefront` through `addViteApp` with external HTTP endpoints), §9 out of scope.
- PLAN.md Phase 5 → Storefront, spec 1 (`01-storefront-shell.md`). It replaces the old PLAN 1.3 and the storefront part of 1.4. Its references to services move to storefront 02, 03 and 11.
