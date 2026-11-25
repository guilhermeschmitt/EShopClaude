# storefront 06 — product search

**Status:** Done (ca76da0)

## Goal
Let the buyer search the product grid on `/` by name, with a vue-base search box next to the category select. A search runs on submit (Enter or a **Search** button), never while typing. Like `page` and `category`, the term lives in the URL (`/?category=Drinkware&q=mug`), so a reload, a shared link and back/forward keep it. A new search keeps the category and resets to page 1, a new category keeps the search and resets to page 1, and the pager keeps both. The matching itself is done by Catalog (`GET /products?q=`, a case-insensitive "contains" match on the name), never in the browser. This completes the "filter by category, search and page through products" step of the journey (PROJECT.md §10).

## Context
- storefront/05 is merged (94728cc). `src/vue-front/storefront/src/views/HomeView.vue`:
  - `PAGE_SIZE = 8`; `service = new CatalogService()` and `categoryService = new CategoryService()` in setup; `useRoute()`/`useRouter()`;
  - local helpers `parsePage(value)` (1-based; anything but `^[1-9]\d*$` is page 1), `parseCategory(value)` (a string with `v.trim() !== ''` is returned as typed, anything else, including a repeated param, is `''`) and `homeQuery(c, n)` → `{ category?: c, page?: String(n) }` (`category` only when set, `page` only past 1);
  - `page` and `category` are computeds over `route.query`; refs `products`, `totalPages`, `loading`, `failed`, and a `let latest` request counter;
  - `load()` calls `service.listProducts({ page: page.value - 1, size: PAGE_SIZE, ...(category.value ? { category: category.value } : {}) })`, drops stale responses (latest wins), and on failure calls `showError('Could not load products', error)`;
  - `watch([page, category], () => void load(), { immediate: true })`;
  - `onSelect(n)` (pager): the current page retries only when `failed`; otherwise `router.push({ name: 'home', query: homeQuery(category.value, n) })`;
  - categories load once in `onMounted`; `onCategory(value)` returns when unchanged, else pushes `homeQuery(c, 1)`;
  - template: `<h1>Products</h1>`, `div.product-filters` (scoped style `max-width: 260px; margin-bottom: 1rem`) holding the `ESelect#category-filter` (`data-testid="category-filter"`), then skeletons / `ul.product-grid[aria-label="Products"]` of `li[data-testid="product-card"]` / `<EEmptyState v-else-if="!failed" icon="inventory_2" title="No products found" />`, then `<ProductPager v-if="totalPages > 1" …>`.
- `src/services/CatalogService.ts`: `listProducts(params: { page: number; size: number; category?: string })` → `request.get('', { params })` on `/api/catalog/products`. Axios serialises the params in object order and omits `undefined` ones.
- AppHost and `vite.config.ts` already proxy `/api/catalog` (storefront/03). This spec changes neither.
- Catalog (catalog/03): `GET /products?q=<text>` trims `q`, ignores it when blank, and matches `lower(name) LIKE %lower(q)%` with `%`, `_` and `\` matched literally. It is combined with `category` by AND. Results are ordered by name. With the seed (24 products), `q=shirt` → `Classic Logo T-Shirt`, `Minimalist Black T-Shirt` (Apparel); `q=mug` → `Ceramic Mug`, `Enamel Camp Mug` (Drinkware); `q=e` → 20 products (`totalPages: 3` at size 8); `category=Apparel&q=e` → 4 products; `category=Bags&q=mug` → `items: []`, `totalPages: 0`.
- vue-base `v0.0.1` (read from `node_modules/@guilherme/vue-base/dist/my-vue-core.js`):
  - `EInputText`: `inheritAttrs: false`; props `id`, `label`, `required`, `error`, `help`, `disabled`, `loading`, and `v-model` (`modelValue: string`, default `''`). It renders `EInputWrapper` (a `<label for=id>` with the `label`) around `<input id type="text" autocomplete="off" …$attrs>`. `$attrs` are merged last, so `type="search"` and `data-testid` land on the inner `<input>` and override `type="text"`.
  - `EButton`: props `label`, `type` (colour, default `primary`), `nativeType` (`'button'` default, `'submit'` submits the enclosing `<form>`, including on Enter in a text field), `disabled`, `loading`, `prependIcon`, among others.
  - `EEmptyState`: props `icon`, `title`, `description` (a second `<p>` under the title, hidden when empty).

## Depends on
- `storefront/05-category-filter`
- `catalog/03-list-products`

## Scope
- [ ] URL state: `/?q=<s>` is the source of truth next to `category` and `page`. A missing, empty, blank or repeated `q` means "no search". Any other value is sent to Catalog exactly as it is in the URL (Catalog trims it). The URL is never rewritten.
- [ ] Products request: `GET /api/catalog/products?page=<N-1>&size=8`, then `&category=<c>` when a category is set, then `&q=<s>` when a search is set. With no search the request is exactly the storefront/05 one, with no `q` param. It reloads whenever `page`, `category` or `q` in the URL changes, and the latest request still wins.
- [ ] Search form: a `<form role="search" aria-label="Product search">` in `.product-filters`, after the category select, holding an `EInputText` (id `product-search`, label `Search`, `type="search"`) and an `EButton` labelled `Search` with `nativeType="submit"`. No clear button.
- [ ] Submitting (Enter in the box or the button) trims the box's text and `router.push`es `/` with `category` kept, `q` set to the trimmed text (omitted when empty) and no `page`, so results start at page 1. Submitting when the trimmed text equals the URL's search does nothing (no navigation, no request). There is no search while typing.
- [ ] The box mirrors the URL: on load, reload, deep link and back/forward it shows the URL's search (`''` when none). Text typed but not submitted is replaced when the URL's search changes.
- [ ] Changing the category keeps `q` and drops `page` (`/?category=<c>&q=<s>`). The pager keeps both (`/?category=<c>&q=<s>&page=N`) and follows the searched `totalPages`.
- [ ] Empty state: when the products load with no items, `EEmptyState` keeps the title `No products found`, and when a search is set it adds the description `No products match "<s>".` (the URL's search). Without a search the empty state is unchanged. A failed load still shows no empty state.

## Explicit non-goals
- No search on description or category. No client-side filtering of products.
- No search-as-you-type, debounce or `router.replace`; no suggestions, autocomplete or highlighting.
- No clear button (to clear, empty the box and submit), and no minimum term length.
- No URL rewrite or normalisation of `q` (an untrimmed deep link stays as it is).
- No caching or shared store for products. The state lives in `HomeView.vue` and the URL.
- No changes to `ProductPager.vue`, `CategoryService.ts`, `format.ts`, `errors.ts`, `routes.ts`, `main.ts`, `vite.config.ts` or `src/apphost.mts`.
- No dependency changes (`package.json`, `package-lock.json`).
- Prices come only from what Basket and Ordering return. No page asks Catalog for a price again, and no request body sends a price to any service (adding sends only `productId` and `quantity`).
- Checkout is one call to Basket (`POST /basket/{buyerId}/checkout`). No page calls Ordering to create anything (no `POST /orders`).
- No page reads a database. The browser talks only to the storefront origin, through `/api/{service}`.
- No changes to catalog, basket, ordering, postgres, redis or their AppHost registrations.
- No AppHost reference to a service this spec's pages don't call: no `services__ordering__*`.
- Nothing from PROJECT.md §9: no login or auth, payments, shipping, product admin or images, stock, order status, checkout form, re-pricing, retries or fallbacks, order paging, or frontend tests.

## Data model
None on the server, and no browser storage (`localStorage`/`sessionStorage`).

**URL state** on route `home` (`/`), with `page` and `category` parsed as in storefront/04 and 05:

| Query | Meaning | API params |
|---|---|---|
| (none) | All, no search, page 1 | `page=0&size=8` |
| `q=<s>`, `s` a non-blank string | search `s` (as in the URL), page 1 | `page=0&size=8&q=<s>` |
| `q=` blank, or repeated `q` | no search | no `q` |
| `category=<c>&q=<s>&page=N` | category `c`, search `s`, page N | `page=<N-1>&size=8&category=<c>&q=<s>` |

Pushed URLs always have their keys in the order `category`, `q`, `page`, each present only when set (`page` only past 1).

**Responses read:** `GET /api/catalog/products` → `ProductPage` (unchanged): `items` for the cards and `totalPages` for the pager.

**Service change:** `CatalogService.listProducts(params: { page: number; size: number; category?: string; q?: string })`. `q` is omitted from the request when it's `undefined`.

**Page state** added to `HomeView.vue`:

| Name | Type | Meaning |
|---|---|---|
| `search` | `ComputedRef<string>` | Search parsed from `route.query.q`; `''` means no search |
| `searchText` | `Ref<string>` | The box's text (`v-model`); set from `search` whenever `search` changes, and initially |
| `emptyDescription` | `ComputedRef<string>` | `search ? \`No products match "${search}".\` : ''` |

## Tasks
1. **CatalogService** (`src/services/CatalogService.ts`): widen `listProducts`'s `params` type to `{ page: number; size: number; category?: string; q?: string }`. No other change.
2. **URL parsing** (`HomeView.vue`): rename `parseCategory` to `parseFilter` (same body; doc comment: "`?category=` / `?q=` are sent to Catalog as typed; missing, blank or repeated means unset (`''`)") and use it for both: `category = computed(() => parseFilter(route.query.category))`, `search = computed(() => parseFilter(route.query.q))`. Change `homeQuery` to `homeQuery(c: string, s: string, n: number)` returning `{ ...(c ? { category: c } : {}), ...(s ? { q: s } : {}), ...(n === 1 ? {} : { page: String(n) }) }`.
3. **Products loading** (`HomeView.vue`): in `load()`, add `...(search.value ? { q: search.value } : {})` after the `category` spread. Change the watch to `watch([page, category, search], () => void load(), { immediate: true })`. The rest of `load()` is unchanged.
4. **Navigation callers** (`HomeView.vue`): `onSelect(n)` pushes `homeQuery(category.value, search.value, n)`; `onCategory(value)` pushes `homeQuery(c, search.value, 1)`. Their early-return rules are unchanged.
5. **Search box state** (`HomeView.vue`): `const searchText = ref('')`; `watch(search, (s) => { searchText.value = s; }, { immediate: true })`. `function onSearch(): void { const s = searchText.value.trim(); if (s === search.value) return; void router.push({ name: 'home', query: homeQuery(category.value, s, 1) }); }`.
6. **Empty state** (`HomeView.vue`): `emptyDescription` as in the Data model, and `<EEmptyState v-else-if="!failed" icon="inventory_2" title="No products found" :description="emptyDescription" />`.
7. **Template** (`HomeView.vue`): in `div.product-filters`, wrap the existing `ESelect` in `<div class="product-filters__category">` (unchanged otherwise), then add `<form class="product-search" role="search" aria-label="Product search" @submit.prevent="onSearch">` holding `<EInputText id="product-search" v-model="searchText" label="Search" type="search" data-testid="product-search" />` and `<EButton label="Search" native-type="submit" prepend-icon="search" data-testid="product-search-submit" />`. Scoped style: `.product-filters { display: flex; flex-wrap: wrap; align-items: flex-end; gap: 1rem; margin-bottom: 1rem; }` (replacing `max-width: 260px`), `.product-filters__category { width: 260px; max-width: 100%; }`, `.product-search { display: flex; align-items: flex-end; gap: 0.5rem; flex: 1 1 260px; max-width: 420px; }`, `.product-search > :first-child { flex: 1; }`. Grid, skeleton and pager markup stay as they are.

## Files touched
- `src/vue-front/storefront/src/services/CatalogService.ts` (modified: `listProducts` params type only)
- `src/vue-front/storefront/src/views/HomeView.vue` (modified)

No AppHost, `vite.config.ts`, component or dependency changes. Nothing in catalog, basket or ordering changes.

## Definition of Done
Run every check from the repository root unless it says otherwise. `<web>` is the storefront URL from the dashboard (storefront → Endpoints). Browser checks use the Playwright MCP (`browser_navigate`, `browser_navigate_back`, `browser_click`, `browser_type`, `browser_press_key`, `browser_snapshot`, `browser_console_messages`, `browser_network_requests`, `browser_evaluate`, `browser_run_code_unsafe`). Definitions:
- `NAMES` is `[...document.querySelectorAll('[data-testid="product-card"] h2')].map(e => e.textContent.trim())`.
- `BOX` is `document.querySelector('[data-testid="product-search"]').value`, the text in the search box.
- `SELECTED` is `document.querySelector('[data-testid="category-filter"]').value`.
- `CURRENT` is `document.querySelector('[data-testid="pager-page"][aria-current="page"]')?.textContent.trim()`.
- **Search `X`** means `browser_type` into the textbox `Search` with the text `X` (replacing its content) and `submit: true` (presses Enter).
- **Pick `X`** means `browser_click` the combobox `Category`, then `browser_click` the option `X` in the listbox.
- **curl(`params`)** means `curl -s '<web>/api/catalog/products?<params>'`.

1. **AppHost build:** in `src/`, `npm run build` exits 0 with no eslint or tsc errors.
2. **Storefront build:** in `src/vue-front/storefront`, `npm ci && npm run build` exits 0, `vue-tsc` reports no errors, and `dist/index.html` exists. Since no Java service is touched, there's no `mvn test`.
3. **Healthy start:** in `src/`, `aspire run` shows every resource (postgres, catalogdb, orderingdb, pgAdmin, redis, catalog, basket, ordering, storefront) as Running/Healthy.
4. **Form:** `browser_navigate` to `<web>/`. The snapshot shows a `search` landmark `Product search` with a textbox `Search` (`document.querySelector('#product-search').type` is `"search"`, `BOX` is `""`) and a button `Search`, next to the combobox `Category`, then the `Products` list with 8 items and the pager.
5. **Search:** search `shirt`. `location.search` is `?q=shirt`, `BOX` is `"shirt"`, and `NAMES` equals the `items[].name` of curl(`q=shirt&page=0&size=100`) (`["Classic Logo T-Shirt","Minimalist Black T-Shirt"]` with the seed). No pager is shown. Search `SHIRT`: `location.search` is `?q=SHIRT` and `NAMES` is the same list (case-insensitive).
6. **Button and trimming:** `browser_navigate` to `<web>/`, `browser_type` `  mug  ` into the textbox `Search` without submitting, then `browser_click` the button `Search`. `location.search` is `?q=mug`, `BOX` is `"mug"`, and `NAMES` equals curl(`q=mug&page=0&size=100`) names (`Ceramic Mug`, `Enamel Camp Mug`).
7. **No search while typing, no-op submit:** on `<web>/?q=mug`, note the number of `/api/catalog/products` requests in `browser_network_requests`. `browser_type` `mu` without submitting: no new products request and the URL is unchanged. Search `mug` again (same term): still no new products request and `history.length` is unchanged. On `<web>/`, submitting an empty box (or `   `) sends no products request and the URL stays `<web>/`.
8. **Combined:** `browser_navigate` to `<web>/?category=Drinkware`, search `mug`. `location.search` is `?category=Drinkware&q=mug`, `SELECTED` is `"Drinkware"`, a request `…/products?page=0&size=8&category=Drinkware&q=mug` is sent, and `NAMES` equals curl(`category=Drinkware&q=mug&page=0&size=100`) names. Pick `Bags`: `location.search` is `?category=Bags&q=mug`, `BOX` is still `"mug"`, and the page shows `No products found` with `No products match "mug".`, no cards and no pager (curl(`category=Bags&q=mug&page=0&size=8`) has `items: []`).
9. **Reset to page 1 and the pager:** `browser_navigate` to `<web>/?page=2`, search `e`. `location.search` is `?q=e` (no `page`), `CURRENT` is `"1"`, and the pager shows `totalPages` from curl(`q=e&page=0&size=8`) pages (3 with the seed). Click `Page 2`: `location.search` is `?q=e&page=2`, a request with `page=1&size=8&q=e` is sent, and `NAMES` equals curl(`q=e&page=1&size=8`) names. Pick `Apparel`: `location.search` is `?category=Apparel&q=e`, `NAMES` equals curl(`category=Apparel&q=e&page=0&size=100`) names (4 with the seed), and no pager is shown.
10. **Clear:** on `<web>/?category=Apparel&q=e`, clear the textbox `Search` and submit (`browser_type` `""` with `submit: true`). `location.search` is `?category=Apparel`, `BOX` is `""`, and the 6 Apparel cards show. A request `…/products?page=0&size=8&category=Apparel` with no `q` is sent.
11. **History and deep link:** `browser_navigate` to `<web>/`, search `shirt`, then search `mug`. `browser_navigate_back` shows `?q=shirt` with `BOX` `"shirt"` and the shirt names; back again shows `<web>/` with `BOX` `""` and page 1's names (curl(`page=0&size=8`)). Type `zzz` without submitting, then run `history.forward()` with `browser_evaluate`: the URL is `?q=shirt` and `BOX` is `"shirt"` (the unsubmitted text is replaced). `browser_navigate` to `<web>/?category=Stationery&q=pen` shows `SELECTED` `"Stationery"`, `BOX` `"pen"` and curl(`category=Stationery&q=pen&page=0&size=100`) names; a reload keeps them.
12. **Blank, repeated and literal `q`:** `browser_navigate` to `<web>/?q=`, `<web>/?q=%20` and `<web>/?q=a&q=b` each show `BOX` `""`, page 1's names and a products request with no `q` param. `browser_navigate` to `<web>/?q=%25` sends `…&q=%25`, which returns 200 with `items: []`, and the page shows `No products found` with `No products match "%".`. `browser_navigate` to `<web>/?category=bags` (unknown category, no search) shows `No products found` with no description (`document.querySelector('.e-empty-state__description')` is `null`).
13. **Network:** `browser_navigate` to `<web>/`, search `shirt`, then pick `Apparel`. `browser_network_requests` lists, in order, exactly these catalog requests, all `GET`, 200 and same-origin: `<web>/api/catalog/products?page=0&size=8` and `<web>/api/catalog/categories` (in either order, one each), then `…/products?page=0&size=8&q=shirt`, then `…/products?page=0&size=8&category=Apparel&q=shirt`. There's exactly one `/categories` request, one `GET <web>/api/basket/basket/<buyerId>`, no request to catalog's port directly, and no `/api/ordering`.
14. **Products error under a search:** on `<web>/?q=mug` with the cards loaded, `browser_run_code_unsafe` runs `await page.route('**/api/catalog/products**', r => r.fulfill({ status: 502, body: '' }));`, then search `shirt`. The snapshot shows an `alert` with `Could not load products` and `HTTP 502`, no cards, no skeletons and no `No products found`, the URL is `?q=shirt`, and `BOX` is `"shirt"`. Then `await page.unrouteAll()` and reload: the shirt cards show.
15. **Console:** `browser_console_messages` across checks 4–14 has no errors or warnings from the app, apart from the failed 502 requests in check 14.
16. **Scope fence:** `git diff --stat main...HEAD` lists only `src/vue-front/storefront/src/views/HomeView.vue`, `src/vue-front/storefront/src/services/CatalogService.ts` and files under `_agenticdev/`. `git diff main...HEAD -- src/apphost.mts src/vue-front/storefront/vite.config.ts src/vue-front/storefront/src/components src/vue-front/storefront/src/services/CategoryService.ts src/vue-front/storefront/package.json` prints nothing. `grep -n "localStorage\|sessionStorage\|\.filter(\|setTimeout\|debounce" src/vue-front/storefront/src/views/HomeView.vue` prints nothing (no browser storage, no client-side filtering, no search-as-you-type).

## References
- PROJECT.md §4.1 Catalog (`GET /products?category=&q=&page=0&size=8`, `q` a case-insensitive match on name, ordered by name), §6 Storefront (`/` has a category select, a search box and a pager, calls `GET /products`; vue-base components), §2 Architecture (browser only talks to `/api/{service}`, errors propagate to the UI, no retries), §9 out of scope (no frontend tests), §10 Definition of done (journey step 1: "filter by category, search and page through products").
- PLAN.md Phase 5 → Storefront, spec 6 (`06-product-search.md`), part of the old 5.2.
- storefront/05 (`05-category-filter.md`): URL state for `category`, `homeQuery`, `parseCategory`, `.product-filters`, the `ESelect`.
- storefront/04 (`04-product-pager.md`): URL state for `page`, `load()` with latest-wins, `ProductPager`, error toast.
- catalog/03 (`03-list-products.md`): `GET /products` with `q` (trimmed, blank ignored, LIKE wildcards literal).
