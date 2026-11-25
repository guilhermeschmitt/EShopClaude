# storefront 05 — category filter

**Status:** Done (94728cc)

## Goal
Let the buyer narrow the product grid on `/` to one category, chosen from a vue-base `ESelect` filled from Catalog's `GET /categories` with an "All" option. Like the page, the category lives in the URL (`/?category=Bags`), so a reload, a shared link and back/forward keep it. Choosing a category resets to page 1, and the pager follows the filtered `totalPages`. The filtering itself is done by Catalog (`GET /products?category=`), never in the browser. Search (storefront/06) adds `q` the same way later.

## Context
- storefront/04 is merged (1711aaf). `src/vue-front/storefront/src/views/HomeView.vue`:
  - `PAGE_SIZE = 8`; `const service = new CatalogService()` in setup; `useRoute()`/`useRouter()`;
  - `page = computed(() => parsePage(route.query.page))` (1-based; anything but `^[1-9]\d*$` is page 1);
  - refs `products`, `totalPages` (`0` until the first success, kept after a failure), `loading`, `failed`, and a plain `let latest` request counter; `async function load()` drops any response whose id is not `latest`, and on failure calls `showError('Could not load products', error)`;
  - `watch(page, () => void load(), { immediate: true })`;
  - `onSelect(n)`: the current page retries only when `failed`; otherwise `router.push({ name: 'home', query: n === 1 ? {} : { page: String(n) } })`;
  - template: `<h1>Products</h1>`, then skeletons (`div.product-grid` of 8 `<ECard loading />`) / `ul.product-grid[aria-label="Products"]` of `li[data-testid="product-card"]` / `EEmptyState` `No products found` (when not `failed`) / nothing, then `<ProductPager v-if="totalPages > 1" …>`.
- `src/components/ProductPager.vue`: props `page`, `totalPages`, `disabled`; emits `select(n)`. This spec does not change it.
- `src/services/CatalogService.ts`: `listProducts(params: { page: number; size: number }): Promise<ProductPage>`, through an `ApiService` whose resource is `products` (`request.get('', { params })` → `/api/catalog/products`). vue-base `ApiService` builds one axios instance per `baseURL` + `resource`, so another resource needs its own service class.
- `src/errors.ts`: `showError(message, error)` adds a sticky error toast with the detail `HTTP <status> <statusText>`.
- AppHost and `vite.config.ts` already proxy `/api/catalog` (storefront/03). This spec doesn't change either one.
- Catalog (catalog/03, catalog/05): `GET /products?category=<c>&page=&size=` is an exact, case-sensitive match on the category, and a blank `category` is ignored. `GET /categories` returns `string[]`, distinct and sorted. The seed has 4 categories (`Apparel`, `Bags`, `Drinkware`, `Stationery`) with 6 products each, so a filtered list fits on one page (`totalPages: 1`) and the pager hides. An unknown category returns `200` with `items: []`, `totalPages: 0`.
- vue-base `v0.0.1` `ESelect` (read from `node_modules/@guilherme/vue-base/dist/my-vue-core.js`): `inheritAttrs: false`, and `$attrs` go to its inner `<input type="text" readonly role="combobox" aria-haspopup="listbox">`. The `id` prop ties the visible `label` to that input. Other props are `options`, `optionLabel` (default `'label'`), `optionValue` (default `'value'`), `placeholder`, `disabled`, `showClear` (default `true`), `loading` and `loadingLabel` (default `"Carregando..."`), and it uses `v-model`. A click opens a `ul[role="listbox"]` teleported to `body`, with one `li[role="option"][aria-selected]` per option holding a `button.e-select__option`. Picking one sets the model and closes the list. While `loading`, it shows a spinner and `loadingLabel` and won't open. The clear button's `aria-label` is Portuguese (`Limpar seleção`), so `showClear` is turned off. When the model matches no option, the input shows the `placeholder`.

## Depends on
- `storefront/04-product-pager`
- `catalog/05-list-categories`

## Scope
- [ ] URL state: `/?category=<c>` is the source of truth next to `?page`. A missing, empty, blank or repeated `category` means "All". Any other value is sent to Catalog exactly as typed. An unknown or wrong-case value gets `items: []` from Catalog, so the page shows `No products found`, and the URL is not rewritten.
- [ ] Products request: `GET /api/catalog/products?page=<N-1>&size=8`, plus `&category=<c>` when a category is set. With "All" the request is exactly the storefront/04 one, with no `category` param. It reloads whenever `page` or `category` in the URL changes, and the latest request still wins.
- [ ] Category select: an `ESelect` labelled `Category` above the grid, with the options `All` (value `''`) followed by each category from `GET /api/catalog/categories` in the order returned. It shows the URL's category, or `All` when there is none.
- [ ] Choosing a category `router.push`es `/?category=<c>` with no `page`, so it resets to page 1. Choosing `All` pushes `/` (or keeps only the other query params; there are none yet). Choosing the category already selected does nothing.
- [ ] The pager keeps `category` in the URL when it changes page (`/?category=<c>&page=N`) and follows the filtered `totalPages`, so it's hidden when the filtered list fits on one page.
- [ ] Categories load once on mount. While they load, the select shows `loading` with the label `Loading…`. On failure: the toast `Could not load categories`, and the select offers only `All`. The grid loads independently, and a `?category=` in the URL still filters. Nothing retries; a reload is the retry.

## Explicit non-goals
- No search box or `q` parameter (storefront/06). No client-side filtering of products, and no product counts per category.
- No multi-select, clear button or option search in the select (`multiple`, `showClear`, `filter` stay off).
- No redirect or URL rewrite for unknown, wrong-case or blank categories.
- No caching or shared store for categories or products. The state lives in `HomeView.vue` and the URL.
- No changes to `ProductPager.vue`, `format.ts`, `errors.ts`, `routes.ts`, `main.ts`, `vite.config.ts` or `src/apphost.mts`.
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

| Query | Meaning | API params |
|---|---|---|
| (none) | All, page 1 | `page=0&size=8` |
| `category=<c>`, `c` a non-blank string | category `c` (as typed), page 1 | `page=0&size=8&category=<c>` |
| `category=` blank, or repeated `category` | All | no `category` |
| `category=<c>&page=N` | category `c`, page N (`page` parsed as in storefront/04) | `page=<N-1>&size=8&category=<c>` |

**Responses read:**
- `GET /api/catalog/categories` → `string[]` (sorted, distinct).
- `GET /api/catalog/products` → `ProductPage` (unchanged): `items` for the cards and `totalPages` for the pager.

**Service changes:**
- `CatalogService.listProducts(params: { page: number; size: number; category?: string })`. `category` is omitted from the request when it's `undefined`, so axios sends `?page=&size=` exactly as before.
- `CategoryService` (new): `config()` → `{ baseURL: \`${Core.baseURL}/catalog\`, resource: 'categories' }`; `list(): Promise<string[]>` via `request.get('')`.

**Page state** added to `HomeView.vue`:

| Name | Type | Meaning |
|---|---|---|
| `category` | `ComputedRef<string>` | Category parsed from `route.query.category`; `''` means All |
| `categories` | `Ref<string[]>` | Result of `GET /categories`; `[]` until loaded and after a failure |
| `categoriesLoading` | `Ref<boolean>` | `true` while the categories request is in flight |
| `categoryOptions` | `ComputedRef<{ label: string; value: string }[]>` | `[{ label: 'All', value: '' }, ...categories.map(c => ({ label: c, value: c }))]` |

## Tasks
1. **CatalogService** (`src/services/CatalogService.ts`): widen `listProducts`'s `params` type to `{ page: number; size: number; category?: string }`. No other change.
2. **CategoryService** (`src/services/CategoryService.ts`, created): `class CategoryService extends ApiService`, with `config()` as in the Data model and `async list(): Promise<string[]>` returning `response.data` of `this.request.get('')`, documented like `CatalogService` (construct only after `Core.init`).
3. **URL parsing** (`HomeView.vue`): `category = computed(() => parseCategory(route.query.category))`, where `parseCategory` returns `v` when `v` is a string with `v.trim() !== ''`, else `''`. A local function next to `parsePage`. Add a local helper `homeQuery(c: string, n: number)` that returns `{ ...(c ? { category: c } : {}), ...(n === 1 ? {} : { page: String(n) }) }`.
4. **Products loading** (`HomeView.vue`): in `load()`, call `service.listProducts({ page: page.value - 1, size: PAGE_SIZE, ...(category.value ? { category: category.value } : {}) })`. Replace `watch(page, …)` with `watch([page, category], () => void load(), { immediate: true })`. The rest of `load()` (latest-wins, failure handling) is unchanged.
5. **Pager** (`HomeView.vue`): `onSelect(n)` keeps its retry rule and pushes `{ name: 'home', query: homeQuery(category.value, n) }`.
6. **Categories loading** (`HomeView.vue`): `const categoryService = new CategoryService()` in setup. `onMounted`: `categoriesLoading = true`; `categories = await categoryService.list()`. On failure, `showError('Could not load categories', error)`, leaving `categories` `[]`. Finally `categoriesLoading = false`. This is independent of `load()`; neither waits for the other.
7. **Choosing a category** (`HomeView.vue`): `onCategory(value: string | null)`. `const c = value ?? ''`. If `c === category.value`, return. Otherwise `void router.push({ name: 'home', query: homeQuery(c, 1) })`.
8. **Template** (`HomeView.vue`): between `<h1>Products</h1>` and the grid branches, add `<div class="product-filters">` holding `<ESelect id="category-filter" label="Category" :options="categoryOptions" :model-value="category" :loading="categoriesLoading" loading-label="Loading…" :show-clear="false" data-testid="category-filter" @update:model-value="onCategory" />`. Scoped style: `.product-filters { max-width: 260px; margin-bottom: 1rem; }`. Grid, skeleton, empty state and pager markup stay as they are.

## Files touched
- `src/vue-front/storefront/src/services/CategoryService.ts` (created)
- `src/vue-front/storefront/src/services/CatalogService.ts` (modified: `listProducts` params type only)
- `src/vue-front/storefront/src/views/HomeView.vue` (modified)

No AppHost, `vite.config.ts` or dependency changes. Nothing in catalog, basket or ordering changes.

## Definition of Done
Run every check from the repository root unless it says otherwise. `<web>` is the storefront URL from the dashboard (storefront → Endpoints). Browser checks use the Playwright MCP (`browser_navigate`, `browser_navigate_back`, `browser_click`, `browser_snapshot`, `browser_console_messages`, `browser_network_requests`, `browser_evaluate`, `browser_run_code_unsafe`). Definitions:
- `NAMES` is `[...document.querySelectorAll('[data-testid="product-card"] h2')].map(e => e.textContent.trim())`.
- `CATS` is `[...document.querySelectorAll('[data-testid="product-card"] .product-card__category')].map(e => e.textContent.trim())`.
- `SELECTED` is `document.querySelector('[data-testid="category-filter"]').value`, the text shown in the select's input.
- `CURRENT` is `document.querySelector('[data-testid="pager-page"][aria-current="page"]')?.textContent.trim()`.
- **Pick `X`** means `browser_click` the combobox `Category`, then `browser_click` the option `X` in the listbox.

1. **AppHost build:** in `src/`, `npm run build` exits 0 with no eslint or tsc errors.
2. **Storefront build:** in `src/vue-front/storefront`, `npm ci && npm run build` exits 0, `vue-tsc` reports no errors, and `dist/index.html` exists. Since no Java service is touched, there's no `mvn test`.
3. **Healthy start:** in `src/`, `aspire run` shows every resource (postgres, catalogdb, orderingdb, pgAdmin, redis, catalog, basket, ordering, storefront) as Running/Healthy.
4. **Options:** `browser_navigate` to `<web>/`. The snapshot shows a combobox `Category` whose `SELECTED` is `"All"`, the `Products` list with 8 items and the three pager buttons. Clicking the combobox shows a listbox whose options, in order, are `All` followed by exactly the array from `curl -s <web>/api/catalog/categories` (`["Apparel","Bags","Drinkware","Stationery"]` with the seed). Press `Escape` to close it.
5. **Filter:** pick `Bags`. `location.search` is `?category=Bags`, `SELECTED` is `"Bags"`, every entry of `CATS` is `"Bags"` (`textContent` is not affected by the CSS uppercase), and `NAMES` equals the `items[].name` of `curl -s '<web>/api/catalog/products?category=Bags&page=0&size=100'`, with its `totalItems` (6) equal to `NAMES.length`. No pager is shown (`document.querySelector('[data-testid="pager-page"]')` is `null`). Repeat for `Apparel`: the same checks with `Apparel`.
6. **Reset and All:** `browser_navigate` to `<web>/`, click `Page 2` (URL `<web>/?page=2`, `CURRENT` `"2"`), then pick `Drinkware`. `location.search` is `?category=Drinkware` with no `page`, and the Drinkware cards show. Pick `All`. The URL is `<web>/` (`location.search` `""`), `SELECTED` is `"All"`, `NAMES` equals page 1's names from `curl -s '<web>/api/catalog/products?page=0&size=8'`, and the pager shows `Page 1`–`Page 3` with `CURRENT` `"1"`.
7. **History and deep link:** after check 6, `browser_navigate_back` shows `?category=Drinkware` with `SELECTED` `"Drinkware"` and Drinkware's names. Back again shows `?page=2` with `SELECTED` `"All"` and page 2's names. `browser_navigate` to `<web>/?category=Stationery` shows `SELECTED` `"Stationery"` and only Stationery cards. A reload keeps them.
8. **Pager keeps the category:** `browser_navigate` to `<web>/?category=Bags&page=2`. A request with `page=1&size=8&category=Bags` is sent, and the page shows `No products found` with no pager (Bags has `totalPages: 1`). `browser_navigate` to `<web>/?category=` and `<web>/?category=%20` each show `SELECTED` `"All"`, page 1's names and a request with no `category` param.
9. **Unknown category:** `browser_navigate` to `<web>/?category=bags`. A request `…/products?page=0&size=8&category=bags` returns 200 with `items: []`. The page shows `No products found`, no cards and no pager, the URL stays `?category=bags`, and `SELECTED` is `""`. Picking `All` returns to `<web>/` with page 1's names.
10. **Network:** `browser_navigate` to `<web>/`, pick `Bags`, then pick `All`. `browser_network_requests` lists, in order, exactly these catalog requests, all `GET`, 200 and same-origin: `<web>/api/catalog/products?page=0&size=8` and `<web>/api/catalog/categories` (in either order, one each), then `…/products?page=0&size=8&category=Bags`, then `…/products?page=0&size=8`. There's exactly one `/categories` request (changing the filter doesn't refetch it), one `GET <web>/api/basket/basket/<buyerId>`, no request to catalog's port directly, and no `/api/ordering`.
11. **Categories loading:** `browser_run_code_unsafe` runs `await page.route('**/api/catalog/categories', async r => { await new Promise(f => setTimeout(f, 3000)); await r.continue(); }); await page.goto('<web>/');`. Within 3 s, `browser_evaluate` shows `SELECTED` is `"Loading…"` while the product cards already show (8 `[data-testid="product-card"]`). After the delay, `SELECTED` is `"All"` and the listbox offers the 5 options. Then `await page.unrouteAll()`.
12. **Categories error:** `browser_run_code_unsafe` runs `await page.route('**/api/catalog/categories', r => r.fulfill({ status: 502, body: '' })); await page.goto('<web>/?category=Bags');`. The snapshot shows an `alert` with `Could not load categories` and `HTTP 502`, and still the 6 Bags cards (the products request succeeded). Clicking the combobox shows a listbox with the single option `All`; picking it shows page 1's names at `<web>/`. Then `await page.unrouteAll()`.
13. **Products error under a filter:** on `<web>/?category=Bags` with the cards loaded, `browser_run_code_unsafe` runs `await page.route('**/api/catalog/products**', r => r.fulfill({ status: 502, body: '' }));`, then pick `Apparel`. The snapshot shows an `alert` with `Could not load products`, no cards, no skeletons and no `No products found`, the URL is `?category=Apparel`, and `SELECTED` is `"Apparel"`. Then `await page.unrouteAll()` and reload: the Apparel cards show.
14. **Console:** `browser_console_messages` across checks 4–13 has no errors or warnings from the app, apart from the failed 502 requests in checks 12 and 13.
15. **Scope fence:** `git diff --stat main...HEAD` lists only `src/vue-front/storefront/src/views/HomeView.vue`, `src/vue-front/storefront/src/services/CatalogService.ts`, `src/vue-front/storefront/src/services/CategoryService.ts` and files under `_agenticdev/`. `git diff main...HEAD -- src/apphost.mts src/vue-front/storefront/vite.config.ts src/vue-front/storefront/src/components src/vue-front/storefront/package.json` prints nothing. `grep -n "localStorage\|sessionStorage\|\.filter(" src/vue-front/storefront/src/views/HomeView.vue src/vue-front/storefront/src/services/CategoryService.ts` prints nothing (no browser storage, no client-side filtering).

## References
- PROJECT.md §4.1 Catalog (`GET /products?category=&…`, exact match on category; `GET /categories` → sorted `string[]`), §6 Storefront (`/` has a category select and a pager, calls `GET /products` and `GET /categories`, vue-base components), §2 Architecture (browser only talks to `/api/{service}`, errors propagate to the UI, no retries), §9 out of scope (no frontend tests), §10 Definition of done (journey step 1: "filter by category … and page through products").
- PLAN.md Phase 5 → Storefront, spec 5 (`05-category-filter.md`), part of the old 5.2.
- storefront/04 (`04-product-pager.md`): URL state for `page`, `load()` with latest-wins, `ProductPager`, error toast.
- catalog/05 (`05-list-categories.md`): `GET /categories`.
