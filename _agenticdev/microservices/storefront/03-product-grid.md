# storefront 03 — product grid

**Status:** Not started

## Goal
Show the first page of products on `/` as a grid of cards. This is the first page that calls Catalog, so it adds the Catalog reference and proxy. The pager, category filter, search and **Add to cart** build on this grid (storefront/04–07).

## Context
- storefront/02 is merged (049f4be). `src/vue-front/storefront/` has:
  - `main.ts`: `getBuyerId()`, then `await Core.init(app, router, routes, { baseURL: '/api' })`, then `app.use(router)` and mount.
  - `App.vue`: `<AppHeader />`, `<main class="app-main"><RouterView /></main>`, `<EToast />`, and `void refreshBasket()` in `onMounted`.
  - `components/AppHeader.vue` with the cart badge, `stores/basket.ts`, `services/BasketService.ts`, `buyer.ts`.
  - `errors.ts`: `showError(message, error)` adds a sticky vue-base error toast. The detail is `HTTP <status> <statusText>` or the error message.
  - `views/HomeView.vue` is still a placeholder: `<h1>Products</h1>` only.
- `services/BasketService.ts` shows the client pattern: `class … extends ApiService` with `config()` returning `{ baseURL: `${Core.baseURL}/<service>`, resource }`. `ApiService` reads `Core.baseURL` in its constructor, so it is constructed lazily (after `Core.init`), never at import time.
- `vite.config.ts` has one proxy entry, `/api/basket` → `process.env.services__basket__http__0`, with `changeOrigin: true`, `proxyTimeout: PROXY_TIMEOUT_MS` (10 000) and the prefix stripped by `rewrite`. It adds the entry only when the env var is set. The timeout exists because Aspire's endpoint proxy never answers for a stopped resource; Vite then answers `502 Bad Gateway` (found in storefront/02).
- `src/apphost.mts`: `catalog` is already a `const` (`const catalog = await builder.addDockerfile('catalog', …)`). The storefront chain is `addViteApp('storefront', './vue-front/storefront')` → `.withNpm({ install: true })` → `.withExternalHttpEndpoints()` → `.withHttpHealthCheck({ path: '/' })` → `.withEnvironment('services__basket__http__0', basket.getEndpoint('http'))` → `.waitFor(basket)`. Its comment says catalog and ordering are added by storefront 03 and 11.
- catalog/03 is merged. `GET /products?page=0&size=8` returns `{ items: Product[], page, size, totalItems, totalPages }`, ordered by name then id. `Product = { id, name, description, price, category }`. `price` is a `BigDecimal` serialized as a JSON number (e.g. `17.0`, `19.9`). `description` is a nullable column. The seed (`V2__seed_products.sql`) has 24 products in 4 categories, so `size=8` gives `totalPages: 3`.
- What vue-base `v0.0.1` ships for this page, read from `node_modules/@guilherme/vue-base/dist/my-vue-core.js` (all registered globally by `Core.init`):
  - `ECard`: props `loading` (shows a built-in skeleton in place of the default slot), `transparent`, `flush`, `hideFooterDivider`, `keepMounted`; slots `default`, `footer`, `skeleton`. The body is `div.e-card__body`.
  - `EEmptyState`: props `icon` (default `inbox`), `title`, `description`; renders the title as `p.e-empty-state__title`.
  - `currencyFormatter(n, currency = 'BRL')` always formats as `pt-BR` (`R$ 19,90`), so this app uses its own en-US formatter instead.

## Depends on
- `storefront/01-storefront-shell`
- `catalog/03-list-products`

## Scope
- [ ] AppHost: storefront gets `withEnvironment('services__catalog__http__0', catalog.getEndpoint('http'))` + `waitFor(catalog)` (not `withReference`, which Dockerfile containers reject, PROJECT §7).
- [ ] `vite.config`: proxy `/api/catalog` → `services__catalog__http__0`, with the prefix stripped and the same `proxyTimeout` as basket.
- [ ] Catalog client: `CatalogService` with `listProducts({ page, size })`.
- [ ] Price formatter: `formatPrice(n)` → en-US USD (`$17.00`), reused by later pages.
- [ ] `/`: `GET /products?page=0&size=8` on mount, one vue-base `ECard` per product (name, category, description, price) in a responsive grid.
- [ ] Loading: 8 `<ECard loading>` skeletons. Empty (`items: []`): `EEmptyState` titled `No products found`. Error: the shared toast `Could not load products`, and no cards and no empty state.

## Explicit non-goals
- No pager, category select, search box or **Add to cart** (storefront/04–07). No query-string or route state for the page number.
- No product images, product detail page or link from a card (PROJECT.md §9; no detail route exists).
- No shared store for products: the grid's state lives in `HomeView.vue`.
- Prices come only from what Basket and Ordering return. No page asks Catalog for a price again, and no request body sends a price to any service (adding sends only `productId` and `quantity`). The price on a card is display only.
- Checkout is one call to Basket (`POST /basket/{buyerId}/checkout`). No page calls Ordering to create anything (no `POST /orders`).
- No page reads a database. The browser talks only to the storefront origin, through `/api/{service}`.
- No changes to catalog, basket, ordering, postgres, redis or their AppHost registrations.
- No AppHost reference to a service this spec's pages don't call: no `services__ordering__*`.
- No `withOtlpExporter()` or other OpenTelemetry setup on the storefront (PROJECT.md §2).
- No dependency changes (`package.json`, `package-lock.json`).
- Nothing from PROJECT.md §9: no login or auth, payments, shipping, product admin or images, stock, order status, checkout form, re-pricing, retries or fallbacks, order paging, or frontend tests.

## Data model
None on the server, and no new browser storage.

**Response shape** read from `GET /api/catalog/products?page=0&size=8`, typed in `src/services/CatalogService.ts`:

```ts
export interface Product { id: number; name: string; description: string | null; price: number; category: string }
export interface ProductPage { items: Product[]; page: number; size: number; totalItems: number; totalPages: number }
```

**Page state** in `HomeView.vue` (local refs, no store):

| Ref | Type | Meaning |
|---|---|---|
| `products` | `Ref<Product[]>` | Items of the last successful load; `[]` before and after a failed load |
| `loading` | `Ref<boolean>` | `true` from mount until the request settles |
| `failed` | `Ref<boolean>` | `true` after a failed load; hides the empty state |

**Formatter** in `src/format.ts`: `formatPrice(value: number): string` = `new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value)`, with the formatter created once at module level.

## Tasks
1. **AppHost** (`src/apphost.mts`): on the storefront chain, after `.waitFor(basket)`, add `.withEnvironment('services__catalog__http__0', catalog.getEndpoint('http'))` and `.waitFor(catalog)`. Update the storefront comment: the browser reaches Basket and Catalog through the Vite proxy, and ordering is added by storefront 11. Every builder call is awaited (`no-floating-promises`).
2. **Proxy** (`src/vue-front/storefront/vite.config.ts`): also read `process.env.services__catalog__http__0`. Build `server.proxy` with one entry per service whose URL is set — `/api/<name>` → `{ target, changeOrigin: true, proxyTimeout: PROXY_TIMEOUT_MS, rewrite: (p) => p.replace(/^\/api\/<name>/, '') }` — through a small helper so basket and catalog share one definition. With neither URL set, `server.proxy` is `{}`. Still no `server.port`. Keep the comments accurate.
3. **Catalog client** (`src/services/CatalogService.ts`): the `Product` and `ProductPage` interfaces, and `class CatalogService extends ApiService` with `config()` returning `{ baseURL: `${Core.baseURL}/catalog`, resource: 'products' }` and `async listProducts(params: { page: number; size: number }): Promise<ProductPage>`, which returns `(await this.request.get('', { params })).data`. The request URL is `/api/catalog/products?page=0&size=8`. Constructed in `HomeView`'s setup (after `Core.init`), never at import time.
4. **Formatter** (`src/format.ts`): `formatPrice` as in Data model.
5. **Grid** (`src/views/HomeView.vue`): keep `<h1>Products</h1>`. In `onMounted`, call `listProducts({ page: 0, size: 8 })`; on success set `products` to `items`; on failure set `failed`, keep `products` empty and call `showError('Could not load products', error)`; in both cases set `loading` to `false`. Template, under the heading:
   - while `loading`: a `div.product-grid` with 8 `<ECard loading />`;
   - else if `products.length`: a `ul.product-grid` (`aria-label="Products"`) with one `li` per product (`:key="product.id"`, `data-testid="product-card"`) wrapping an `ECard` whose body has the name as `<h2 class="product-card__name">`, the category, the description (omitted when `null`) and `<p class="product-card__price" data-testid="product-price">{{ formatPrice(product.price) }}</p>`;
   - else if not `failed`: `<EEmptyState icon="inventory_2" title="No products found" />`;
   - else nothing (the toast is the error display).
   - Style: `.product-grid` is a CSS grid with `grid-template-columns: repeat(auto-fill, minmax(220px, 1fr))`, a gap, and no list bullets or padding. Use vue-base tokens with hex fallbacks, as `AppHeader.vue` does. Scoped styles only.

## Files touched
- `src/apphost.mts` (modified: storefront env + wait for catalog, storefront comment)
- `src/vue-front/storefront/vite.config.ts` (modified)
- `src/vue-front/storefront/src/views/HomeView.vue` (modified)
- `src/vue-front/storefront/src/services/CatalogService.ts` (created)
- `src/vue-front/storefront/src/format.ts` (created)

No dependency changes. Nothing in catalog, basket or ordering changes.

## Definition of Done
Run every check from the repository root unless it says otherwise. `<web>` is the storefront URL from the dashboard (storefront → Endpoints). Browser checks use the Playwright MCP (`browser_navigate`, `browser_snapshot`, `browser_console_messages`, `browser_network_requests`, `browser_evaluate`, `browser_run_code_unsafe`).

1. **AppHost build:** in `src/`, `npm run build` exits 0 with no eslint or tsc errors.
2. **Storefront build:** in `src/vue-front/storefront`, `npm ci && npm run build` exits 0, `vue-tsc` reports no errors, and `dist/index.html` exists. Since no Java service is touched, there's no `mvn test`.
3. **Healthy start:** in `src/`, `aspire run` shows every resource (postgres, catalogdb, orderingdb, pgAdmin, redis, catalog, basket, ordering, storefront) as Running/Healthy. storefront starts only after catalog and basket are healthy.
4. **Env:** dashboard → storefront → Environment has `services__catalog__http__0` set to catalog's `http://localhost:<port>` endpoint, still has `services__basket__http__0`, and has no `services__ordering__*`.
5. **Proxy:** `curl -s '<web>/api/catalog/products?page=0&size=8'` returns 200 with 8 `items`, `"page":0`, `"size":8`, `"totalItems":24` and `"totalPages":3`. `curl -s -o /dev/null -w '%{http_code}' <web>/api/basket/basket/00000000-0000-4000-8000-000000000000` still prints `200`.
6. **Grid:** `browser_navigate` to `<web>/`. The snapshot shows the `Products` heading and a list named `Products` with 8 items. `browser_evaluate` → `[...document.querySelectorAll('[data-testid="product-card"] h2')].map(e => e.textContent.trim())` equals the 8 `name`s of the curl in check 5, in the same order (starting `Baseball Cap`, `Canvas Tote Bag`, `Ceramic Mug`). The `Baseball Cap` card shows `Apparel`, its description and `$17.00`; `browser_evaluate` → `document.querySelector('[data-testid="product-price"]').textContent.trim()` is `"$17.00"`.
7. **Console:** `browser_console_messages` after check 6 has no errors or warnings from the app.
8. **Network:** after a reload of `<web>/`, `browser_network_requests` lists exactly two `/api/` requests, both 200 and same-origin: `GET <web>/api/catalog/products?page=0&size=8` and `GET <web>/api/basket/basket/<buyerId>`. No request goes to catalog's or basket's port directly, or to `/api/ordering`.
9. **Loading:** `browser_run_code_unsafe` with `await page.route('**/api/catalog/products**', async r => { await new Promise(f => setTimeout(f, 3000)); await r.continue(); }); await page.reload();`, then within 3 s `browser_evaluate` → `document.querySelectorAll('.product-grid .e-card').length` is `8` and `document.querySelectorAll('[data-testid="product-card"]').length` is `0`. After the delay, the 8 product cards replace the skeletons. Then `await page.unrouteAll()`.
10. **Empty:** `browser_run_code_unsafe` with `await page.route('**/api/catalog/products**', r => r.fulfill({ json: { items: [], page: 0, size: 8, totalItems: 0, totalPages: 0 } })); await page.reload();`. The snapshot shows `No products found`, no product cards and no alert. Then `await page.unrouteAll()`.
11. **Error:** stop `catalog` in the dashboard. `curl -s -m 30 -o /dev/null -w '%{http_code} %{time_total}' '<web>/api/catalog/products?page=0&size=8'` prints `502` after about 10 s, not a hang. Reload `<web>/`: within 15 s the snapshot shows an `alert` with `Could not load products` and the detail `HTTP 502 Bad Gateway`, no product cards, no skeletons and no `No products found`. The header, the cart badge and the `Products` heading still render. The browser console's only error is that failed request. Start `catalog` again, reload, and the 8 cards return with no alert.
12. **Trace:** dashboard → Traces has a `catalog` trace for `GET /products` with a Postgres span on `catalogdb`.
13. **Proxy is conditional:** `grep -n "services__catalog__http__0" src/vue-front/storefront/vite.config.ts` and `grep -n "services__basket__http__0" src/vue-front/storefront/vite.config.ts` both match, `grep -n "proxyTimeout" src/vue-front/storefront/vite.config.ts` matches, and `grep -n "services__ordering" src/vue-front/storefront/vite.config.ts` prints nothing.
14. **Scope fence:** `git diff --stat main...HEAD` lists only `src/apphost.mts`, files under `src/vue-front/storefront/src/`, `src/vue-front/storefront/vite.config.ts` and files under `_agenticdev/`. `git diff main...HEAD -- src/apphost.mts` shows only the two added storefront calls and the storefront comment. `CatalogService.ts` makes only `GET` requests (`grep -nE "request\.(post|put|patch|delete)" src/vue-front/storefront/src/services/CatalogService.ts` prints nothing).

## References
- PROJECT.md §2 Architecture (the browser talks only to the storefront origin, `/api/{service}` proxied to `services__{name}__http__0` with the prefix stripped, errors propagate to the UI, no retries), §4.1 Catalog (`GET /products`, page shape, `Product`, name order, seed of about 24 products), §6 Storefront (`/` is the product grid, vue-base components and bundled axios), §7 Aspire AppHost (storefront → catalog, `withEnvironment` + `waitFor` in place of `withReference` for Dockerfile containers), §9 out of scope (no product images, no frontend tests).
- PLAN.md Phase 5 → Storefront, spec 3 (`03-product-grid.md`), part of the old 5.2. Its "References" bullet says catalog is referenced in 03.
- storefront/02 (`02-cart-badge.md`): the `ApiService` client pattern, `showError`, and the `proxyTimeout` finding.
