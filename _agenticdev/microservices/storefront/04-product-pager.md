# storefront 04 — product pager

**Status:** Done (1711aaf)

## Goal
Let the buyer page through all ~24 products on `/` with numbered page buttons under the grid. The page number lives in the URL (`/?page=2`), so a reload, a shared link and browser back/forward all keep the page. The category filter and search (storefront/05–06) add their own query parameters next to `page` and reset it to page 1.

## Context
- storefront/03 is merged (5044b4b). `src/vue-front/storefront/src/views/HomeView.vue`:
  - builds `const service = new CatalogService()` in setup (after `Core.init`), with `PAGE_SIZE = 8`;
  - holds the local refs `products`, `loading` (starts `true`) and `failed`;
  - calls `service.listProducts({ page: 0, size: PAGE_SIZE })` once in `onMounted`, and on failure sets `failed` and calls `showError('Could not load products', error)`;
  - template: `<h1>Products</h1>`, then 8 `<ECard loading />` in `div.product-grid` while loading, else `ul.product-grid[aria-label="Products"]` of `li[data-testid="product-card"]`, else `EEmptyState` `No products found` when not `failed`, else nothing (the toast is the error display).
- `src/services/CatalogService.ts`: `listProducts(params: { page: number; size: number }): Promise<ProductPage>`, where `ProductPage = { items, page, size, totalItems, totalPages }` and `page` is **zero-based**. This spec does not change it.
- `src/router/routes.ts`: `{ path: '/', name: 'home', component: HomeView }`, using `createWebHistory` in `main.ts`. vue-base's guard only blocks routes with `meta.permission`, so query changes on `/` pass through.
- `src/errors.ts`: `showError(message, error)` adds a sticky error toast with the detail `HTTP <status> <statusText>`.
- AppHost and `vite.config.ts` already proxy `/api/catalog` (storefront/03). This spec doesn't change either one.
- catalog/03: `GET /products?page=N&size=8` with 24 seeded products gives `totalPages: 3`. A page past the end returns `200` with `items: []` and the real `totalPages`.
- vue-base `v0.0.1` (read from `node_modules/@guilherme/vue-base/dist/my-vue-core.js`) has **no standalone pager**. Its paginator is internal to `ETable` (`e-table__paginator`). `EButton` has the props `label`, `type` (`primary`|`secondary`|…), `outlined`, `ghost`, `size` (`small`|`medium`|`large`), `disabled` and `loading`, and emits `click` (never while `disabled`). Extra attributes (`aria-labelledby`, `aria-current`, `data-testid`) fall through to the native `<button>`, but `aria-label` does not: `EButton` always binds its own `aria-label` after `$attrs` (the `label` for icon-only buttons, else `undefined`), so a passed `aria-label` is dropped (found while verifying this spec). The pager is therefore our own component built from `EButton`.

## Depends on
- `storefront/03-product-grid`
- `catalog/03-list-products`

## Scope
- [ ] URL state: `/?page=N` (1-based) is the source of truth. Page 1 is plain `/` with no query. Missing or invalid values (`abc`, `0`, `-1`, `1.5`) mean page 1.
- [ ] `HomeView` loads `GET /api/catalog/products?page=<N-1>&size=8` whenever `route.query.page` changes, including the first render, and ignores any response that is no longer the latest request.
- [ ] `ProductPager` component: a `nav` labelled `Pagination` with one numbered `EButton` per page (`1` … `totalPages`). The current page is filled and has `aria-current="page"`, and the others are outlined.
- [ ] The pager is shown under the grid when the last successful response had `totalPages > 1`. It's hidden before the first success.
- [ ] Clicking a page `router.push`es its URL, so back/forward walk the pages.
- [ ] Loading a page: the same 8 `<ECard loading />` skeletons as storefront/03, and every pager button disabled.
- [ ] Failure: the toast `Could not load products`, no cards, no skeletons and no empty state. The URL keeps the requested page, and the pager stays visible and enabled. Clicking the current page number loads it again (a manual retry; nothing retries automatically).
- [ ] Out of range (`/?page=9`): Catalog returns `items: []`, so the page shows `No products found` and the pager (pages 1–3) so the buyer can go back.

## Explicit non-goals
- No category select or search (storefront/05–06), and no `category`/`q` query parameters yet.
- No Previous/Next/First/Last buttons, ellipsis or page-size selector. `size` stays `8`.
- No redirect or URL rewrite for invalid or out-of-range `page` values. They're read as described in Scope and the URL is left as typed.
- No scroll restoration or scroll-to-top logic, and no product count or "showing X–Y" text.
- No shared store for products or paging. The state lives in `HomeView.vue` and the URL.
- No changes to `CatalogService.ts`, `format.ts`, `errors.ts`, `routes.ts`, `main.ts`, `vite.config.ts` or `src/apphost.mts`.
- Prices come only from what Basket and Ordering return. No page asks Catalog for a price again, and no request body sends a price to any service (adding sends only `productId` and `quantity`). Card prices are display only.
- Checkout is one call to Basket (`POST /basket/{buyerId}/checkout`). No page calls Ordering to create anything (no `POST /orders`).
- No page reads a database. The browser talks only to the storefront origin, through `/api/{service}`.
- No changes to catalog, basket, ordering, postgres, redis or their AppHost registrations.
- No AppHost reference to a service this spec's pages don't call: no `services__ordering__*`.
- No dependency changes (`package.json`, `package-lock.json`).
- Nothing from PROJECT.md §9: no login or auth, payments, shipping, product admin or images, stock, order status, checkout form, re-pricing, retries or fallbacks, order paging, or frontend tests.

## Data model
None on the server, and no browser storage (`localStorage`/`sessionStorage`).

**URL state** on route `home` (`/`):

| Query | Meaning | API `page` |
|---|---|---|
| (none) | page 1 | `0` |
| `page=N`, `N` matching `^[1-9]\d*$` | page N | `N-1` |
| anything else (`abc`, `0`, `-1`, `1.5`, repeated `page`) | page 1 | `0` |

**Response read:** `ProductPage` from `CatalogService` (unchanged): `items` for the cards and `totalPages` for the pager.

**Page state** in `HomeView.vue`:

| Name | Type | Meaning |
|---|---|---|
| `page` | `ComputedRef<number>` | 1-based page parsed from `route.query.page` |
| `products` | `Ref<Product[]>` | Items of the last successful load; `[]` while loading and after a failure |
| `totalPages` | `Ref<number>` | `totalPages` of the last successful load; `0` until the first success; kept after a failure |
| `loading` | `Ref<boolean>` | `true` while the latest request is in flight |
| `failed` | `Ref<boolean>` | `true` when the latest request failed; hides the empty state |
| `latest` | `number` (plain `let`) | Counter of the latest request; a response whose id is not `latest` is dropped |

**`ProductPager.vue` contract:** props `page: number` (1-based), `totalPages: number`, `disabled: boolean`; emits `select(page: number)` when a page button is clicked.

## Tasks
1. **Pager** (`src/vue-front/storefront/src/components/ProductPager.vue`, created): `<nav class="product-pager" aria-label="Pagination">` holding, per `n` in `1..totalPages` (`:key="n"` on a `<template>`), a hidden `<span :id hidden>Page {{ n }}</span>` (ids from Vue's `useId()`) and an `EButton` with `:label="String(n)"`, `:aria-labelledby` pointing at that span (so the accessible name is `Page N` while the visible text stays `N`), `:aria-current="n === page ? 'page' : undefined"`, `:outlined="n !== page"`, `size="small"`, `:disabled="disabled"`, `data-testid="pager-page"` and `@click="emit('select', n)"`. Scoped style: a centred flex row with a gap and top margin, using vue-base tokens with hex fallbacks as `AppHeader.vue` does.
2. **URL parsing** (`HomeView.vue`): `useRoute()`/`useRouter()`. `page = computed(() => parsePage(route.query.page))`, where `parsePage` returns `Number(v)` when `v` is a string matching `^[1-9]\d*$`, else `1` (arrays and `null` give `1`). A local function in the view, not exported.
3. **Loading** (`HomeView.vue`): replace `onMounted` with `async function load()`. It does `const id = ++latest`, sets `loading = true`, `failed = false` and `products = []`, then `await service.listProducts({ page: page.value - 1, size: PAGE_SIZE })`. If `id !== latest` it returns without touching state. On success it sets `products = items` and `totalPages = totalPages`. On failure it sets `failed = true` and calls `showError('Could not load products', error)`. It then sets `loading = false` (only when `id === latest`). Call it with `watch(page, () => void load(), { immediate: true })`, so a second navigation while a request runs supersedes it.
4. **Selecting a page** (`HomeView.vue`): `onSelect(n)`. If `n === page.value`, call `load()` only when `failed` (retry), else do nothing. Otherwise `void router.push({ name: 'home', query: n === 1 ? {} : { page: String(n) } })`.
5. **Template** (`HomeView.vue`): keep the heading, the skeleton/grid/empty/nothing branches and the styles from storefront/03 unchanged. After them add `<ProductPager v-if="totalPages > 1" :page="page" :total-pages="totalPages" :disabled="loading" @select="onSelect" />`, imported locally.

## Files touched
- `src/vue-front/storefront/src/components/ProductPager.vue` (created)
- `src/vue-front/storefront/src/views/HomeView.vue` (modified)

No AppHost, `vite.config.ts` or dependency changes. Nothing in catalog, basket or ordering changes.

## Definition of Done
Run every check from the repository root unless it says otherwise. `<web>` is the storefront URL from the dashboard (storefront → Endpoints). Browser checks use the Playwright MCP (`browser_navigate`, `browser_navigate_back`, `browser_click`, `browser_snapshot`, `browser_console_messages`, `browser_network_requests`, `browser_evaluate`, `browser_run_code_unsafe`). `NAMES` is `[...document.querySelectorAll('[data-testid="product-card"] h2')].map(e => e.textContent.trim())`. `CURRENT` is `document.querySelector('[data-testid="pager-page"][aria-current="page"]')?.textContent.trim()`.

1. **AppHost build:** in `src/`, `npm run build` exits 0 with no eslint or tsc errors.
2. **Storefront build:** in `src/vue-front/storefront`, `npm ci && npm run build` exits 0, `vue-tsc` reports no errors, and `dist/index.html` exists. Since no Java service is touched, there's no `mvn test`.
3. **Healthy start:** in `src/`, `aspire run` shows every resource (postgres, catalogdb, orderingdb, pgAdmin, redis, catalog, basket, ordering, storefront) as Running/Healthy.
4. **Page 1:** `browser_navigate` to `<web>/`. The snapshot shows the `Products` list with 8 items and a navigation `Pagination` with exactly three buttons, `Page 1`, `Page 2` and `Page 3`. `CURRENT` is `"1"`. `NAMES` equals the names of `curl -s '<web>/api/catalog/products?page=0&size=8'`.
5. **Click through:** `browser_click` `Page 2`. The URL (`location.href`) is `<web>/?page=2`, `CURRENT` is `"2"`, and `NAMES` equals the names of `curl -s '<web>/api/catalog/products?page=1&size=8'`. Then click `Page 3`: the URL ends with `/?page=3`, `CURRENT` is `"3"`, and `NAMES` equals the names from `page=2`. The 24 names from the three pages, concatenated in order, equal the `items[].name` of `curl -s '<web>/api/catalog/products?page=0&size=100'`, with no duplicates.
6. **Page 1 URL and history:** from page 3, `browser_navigate_back` shows the `?page=2` URL with page 2's names and `CURRENT` `"2"`. Click `Page 1`: `location.search` is `""`, the URL is `<web>/`, and `NAMES` are page 1's.
7. **Deep link:** `browser_navigate` to `<web>/?page=3` shows page 3's names with `CURRENT` `"3"`. A reload keeps them.
8. **Invalid and out of range:** `<web>/?page=abc`, `<web>/?page=0` and `<web>/?page=-1` each show page 1's names with `CURRENT` `"1"`, the URL left as typed, and a request with `page=0`. `<web>/?page=9` sends `page=8&size=8`, shows `No products found` and no cards, and still shows the three pager buttons with none of them `aria-current`. Clicking `Page 1` then shows page 1's names at `<web>/`.
9. **Network:** `browser_navigate` to `<web>/`, click `Page 2`, then `Page 3`. `browser_network_requests` lists, in order, exactly these catalog requests, all `GET`, 200 and same-origin: `<web>/api/catalog/products?page=0&size=8`, `…?page=1&size=8` and `…?page=2&size=8`. There's one `GET <web>/api/basket/basket/<buyerId>` (page navigation doesn't remount the app), no request to catalog's port directly, and no `/api/ordering`.
10. **Loading:** on `<web>/` with page 1 loaded, `browser_run_code_unsafe` runs `await page.route('**/api/catalog/products**', async r => { await new Promise(f => setTimeout(f, 3000)); await r.continue(); }); await page.getByRole('button', { name: 'Page 2' }).click();`. Then, within 3 s, `browser_evaluate` checks that `document.querySelectorAll('.product-grid .e-card').length` is `8`, that `document.querySelectorAll('[data-testid="product-card"]').length` is `0`, and that `[...document.querySelectorAll('[data-testid="pager-page"]')].every(b => b.disabled)` is `true`. After the delay, page 2's names show and every pager button is enabled. Then `await page.unrouteAll()`.
11. **Stale response dropped:** on `<web>/` with page 1 loaded, `browser_run_code_unsafe` runs `await page.route('**/api/catalog/products?page=1**', async r => { await new Promise(f => setTimeout(f, 3000)); await r.continue(); }); await page.getByRole('button', { name: 'Page 2' }).click(); await page.goBack(); await page.waitForTimeout(4000);`. After that, the URL is `<web>/`, `NAMES` are page 1's and `CURRENT` is `"1"`. Then `await page.unrouteAll()`.
12. **Error and retry:** on `<web>/` with page 1 loaded, `browser_run_code_unsafe` runs `await page.route('**/api/catalog/products**', r => r.fulfill({ status: 502, body: '' })); await page.getByRole('button', { name: 'Page 2' }).click();`. The snapshot then shows an `alert` with `Could not load products` and `HTTP 502`, no product cards, no skeletons and no `No products found`. The URL ends with `/?page=2`, the pager shows three enabled buttons and `CURRENT` is `"2"`. Then `await page.unrouteAll()` and click `Page 2`: page 2's names show (one more `page=1&size=8` request).
13. **Console:** `browser_console_messages` across checks 4–12 has no errors or warnings from the app, apart from the failed 502 request in check 12.
14. **Scope fence:** `git diff --stat main...HEAD` lists only `src/vue-front/storefront/src/views/HomeView.vue`, `src/vue-front/storefront/src/components/ProductPager.vue` and files under `_agenticdev/`. `git diff main...HEAD -- src/apphost.mts src/vue-front/storefront/vite.config.ts src/vue-front/storefront/src/services src/vue-front/storefront/package.json` prints nothing. `grep -n "localStorage\|sessionStorage" src/vue-front/storefront/src/views/HomeView.vue src/vue-front/storefront/src/components/ProductPager.vue` prints nothing.

## References
- PROJECT.md §4.1 Catalog (`GET /products?…&page=0&size=8`, page shape `{ items, page, size, totalItems, totalPages }`, about 24 products so paging is visible), §6 Storefront (`/` is the product grid with a pager, vue-base components), §2 Architecture (browser only talks to `/api/{service}`, errors propagate to the UI, no retries), §9 out of scope (no frontend tests), §10 Definition of done (journey step 1: "page through products").
- PLAN.md Phase 5 → Storefront, spec 4 (`04-product-pager.md`), part of the old 5.2.
- storefront/03 (`03-product-grid.md`): grid, skeleton, empty and error states, `CatalogService`, `showError`.
