# storefront 08 — cart view

**Status:** Not started

## Goal
Show the basket on `/cart`: its lines and total exactly as Basket returns them. The page reads the shared basket state that the header badge already uses, so it sends no request of its own. It gives storefront/09 (quantity +/- and remove) and storefront/10 (**Checkout**) a page to build on, and it makes the "cart" step of the journey visible (PROJECT.md §6, §10).

## Context
- storefront/07 is merged (bd97a18). `src/vue-front/storefront/src/views/CartView.vue` is still the storefront/01 placeholder: only `<h1>Cart</h1>`. The `/cart` route (`name: 'cart'`) already exists in `src/router/routes.ts`, and the header's cart link (`AppHeader.vue`) already points to it.
- `src/stores/basket.ts` (storefront/02 and 07):
  - a module-level `state = ref<Basket | null>(null)`, exported read-only as `basket`, and `itemCount` (the sum of `quantity`);
  - `refreshBasket()` GETs the basket for `getBuyerId()`; on failure it sets `state` to `null` and calls `showError('Could not load the cart', error)`, and it never rejects;
  - `addItem(productId)` sets `state` from the POST response, or on failure shows `showError('Could not add to cart', error)` and leaves `state` alone;
  - `useBasket()` returns `{ basket, itemCount, refreshBasket, addItem }`;
  - the store has no way to tell "not loaded yet" from "load failed", because both are `basket === null`.
- `src/App.vue` calls `refreshBasket()` once in `onMounted`, so the basket is loaded on every full page load, including a direct load of `/cart`. After an add on `/`, the state already holds the basket the POST returned, so `/cart` needs no request of its own.
- `src/services/BasketService.ts` defines `BasketItem = { productId, productName, unitPrice, quantity }` and `Basket = { buyerId, items, total }`. Nothing in it changes here.
- `src/format.ts` exports `formatPrice(value)`, which formats `17` as `$17.00` (en-US, USD).
- vue-base components already used, and verified, in this app: `ECard` (including the `loading` skeleton, used in `HomeView.vue`), `EEmptyState` (`icon`, `title`, `description`), `EButton`, `EIcon` and `EToast`. `EToast` is mounted in `App.vue`, and `showError` shows a sticky error toast whose detail is `HTTP <status> <statusText>`.
- basket/01 is merged (00595ad). `GET /basket/{buyerId}` returns `{ buyerId, items[], total }`, and a buyer with no basket gets `items: []` and `total: 0`. The AppHost already gives the storefront `services__basket__http__0` + `waitFor(basket)`, and `vite.config.ts` already proxies `/api/basket` (storefront/02). Neither one changes here.

## Depends on
- `storefront/02-cart-badge`
- `basket/01-read-basket`

## Scope
- [ ] Store: a read-only `status` (`'loading' | 'ready' | 'failed'`), so a page can tell a basket that is still loading from one that failed to load.
- [ ] `/cart`: while `status` is `'loading'`, an `ECard` skeleton.
- [ ] `/cart`: when the basket has lines, a native `<table>` inside an `ECard` with the columns Product, Unit price, Quantity and Line total, plus a footer row showing Basket's `total`.
- [ ] `/cart`: when the basket has no lines, an `EEmptyState` ("Your cart is empty") and a **Browse products** link to `/`.
- [ ] `/cart`: when `status` is `'failed'`, only the heading. The error toast that `refreshBasket` already shows is the feedback.

## Explicit non-goals
- No +/-, remove or **Checkout** button (storefront/09–10). The page shows Basket's `total` and does not compute its own. A line total (`unitPrice × quantity`) is display-only and is never summed.
- No request from `/cart` itself: no `refreshBasket()` on mount and no new `BasketService` method (decided). No change to `App.vue`, `AppHeader.vue`, `BasketService.ts`, the router, the AppHost, `vite.config.ts` or the dependencies.
- No vue-base table component (decided: a native `<table>`), no inline error message (decided: toast only), and no retry button.
- Prices come only from what Basket and Ordering return. No page asks Catalog for a price again, and no request body sends a price to any service (adding sends only `productId` and `quantity`).
- Checkout is one call to Basket (`POST /basket/{buyerId}/checkout`). No page calls Ordering to create anything (no `POST /orders`).
- No page reads a database. The browser talks only to the storefront origin, through `/api/{service}`.
- No changes to catalog, basket, ordering, postgres, redis or their AppHost registrations.
- No AppHost reference to a service this spec's pages don't call.
- Nothing from PROJECT.md §9: no login or auth, payments, shipping, product admin or images, stock, order status, checkout form, re-pricing, retries or fallbacks, order paging, or frontend tests.

## Data model
None on the server, and no new browser storage. `eshop.buyerId` is still read only through `getBuyerId()`.

**Response read** (unchanged, from the shared state): `Basket = { buyerId: string, items: BasketItem[], total: number }` and `BasketItem = { productId: number, productName: string, unitPrice: number, quantity: number }`.

**Store** (`src/stores/basket.ts`), one new export:

| Export | Type | Meaning |
|---|---|---|
| `status` | `Readonly<Ref<'loading' \| 'ready' \| 'failed'>>` | Starts as `'loading'`. `refreshBasket()` sets it to `'ready'` on success and to `'failed'` on failure. `addItem()` sets it to `'ready'` on success and leaves it alone on failure. Nothing sets it back to `'loading'`, so a later refresh never brings the skeleton back. |

`useBasket()` returns `{ basket, itemCount, status, refreshBasket, addItem }`.

**What `/cart` renders** (`CartView.vue`), after `<h1>Cart</h1>`:

| Condition | Renders |
|---|---|
| `status === 'loading'` | `<ECard loading />` |
| `status === 'failed'` | nothing more |
| `status === 'ready'` and `basket.items.length === 0` | `<EEmptyState icon="shopping_cart" title="Your cart is empty" />` and `<RouterLink to="/">Browse products</RouterLink>` |
| `status === 'ready'` and there are lines | `<ECard>` with `<table class="cart-table" aria-label="Cart lines">`: a header row (Product, Unit price, Quantity, Line total), one `<tr data-testid="cart-line">` per item in Basket's order, and a `<tfoot>` row "Total" with `formatPrice(basket.total)` (`data-testid="cart-total"`) |

Per line: `productName`, `formatPrice(unitPrice)`, `quantity`, and `formatPrice(unitPrice * quantity)` (`data-testid="line-total"`). The rows are keyed by `productId`.

## Tasks
1. **Store** (`src/stores/basket.ts`):
   - add `export type BasketStatus = 'loading' | 'ready' | 'failed';`, `const statusState = ref<BasketStatus>('loading');` and `export const status = readonly(statusState);`, with a doc comment;
   - in `refreshBasket`, set `statusState.value = 'ready'` after `state` is assigned on success, and `'failed'` in the `catch`;
   - in `addItem`, set `'ready'` after a successful assignment;
   - add `status` to `useBasket()`'s return;
   - update the top comment to mention `status`.
2. **Page** (`src/views/CartView.vue`): `<script setup lang="ts">` with `const { basket, status } = useBasket();`, `formatPrice` from `@/format`, and `const items = computed(() => basket.value?.items ?? []);`. Render the template following the "What `/cart` renders" table. Numeric cells are right-aligned.
3. **Styles** (scoped, in `CartView.vue`): `.cart-table { width: 100%; border-collapse: collapse; }`, cell padding about `0.5rem 0.75rem`, a bottom border on the rows using `var(--e-primitive-slate-200, #dee1e7)` as in `AppHeader.vue`, `.cart-table__num { text-align: right; }`, a bold `tfoot` row, and a small top margin on the **Browse products** link.

## Files touched
- `src/vue-front/storefront/src/stores/basket.ts` (modified)
- `src/vue-front/storefront/src/views/CartView.vue` (modified)

No AppHost, `vite.config.ts`, router, service or dependency changes (`package.json` and `package-lock.json` are untouched). Nothing in catalog, basket or ordering changes.

## Definition of Done
Run every check from the repository root unless it says otherwise. `<web>` is the storefront URL from the dashboard (storefront → Endpoints). `B` is the buyerId, read in the browser with the Playwright MCP `browser_evaluate` → `localStorage.getItem('eshop.buyerId')`. In Git Bash, set `B=<that value>`. Browser checks use the Playwright MCP (`browser_navigate`, `browser_snapshot`, `browser_click`, `browser_console_messages`, `browser_network_requests`, `browser_evaluate`).

1. **AppHost build:** in `src/`, `npm run build` exits 0 with no eslint or tsc errors.
2. **Storefront build:** in `src/vue-front/storefront`, `npm ci && npm run build` exits 0, `vue-tsc` reports no errors, and `dist/index.html` exists. Since no Java service is touched, there's no `mvn test`.
3. **Healthy start:** in `src/`, `aspire run` shows every resource (postgres, catalogdb, orderingdb, pgAdmin, redis, catalog, basket, ordering, storefront) as Running/Healthy.
4. **Empty:** on a fresh buyer (`browser_navigate` to `<web>/`, `browser_evaluate` → `localStorage.clear()`), `browser_navigate` to `<web>/cart`. The snapshot shows the heading `Cart`, the text `Your cart is empty` and a link `Browse products`, and no table (`browser_evaluate` → `document.querySelector('.cart-table')` is `null`). Clicking **Browse products** goes to `<web>/` and shows the `Products` heading.
5. **Lines:** on `/`, click **Add to cart** on the first card, then the second card, then the first card again, waiting for each to settle. Then click the header cart link (no reload). The snapshot shows a table `Cart lines` with exactly two `cart-line` rows: the first card's product with quantity `2` and the second card's with quantity `1`. Each row's unit price matches its card's price, and each line total equals unit price × quantity (for example `$19.90` × 2 → `$39.80`).
6. **Total matches Basket:** `curl -s <web>/api/basket/basket/$B` returns `total` T. `browser_evaluate` → `document.querySelector('[data-testid="cart-total"]').textContent.trim()` equals T formatted as en-US dollars (`39.8` → `$39.80`), and the row names and quantities match the `items` in the response, in the same order.
7. **No request from the page:** after the client-side navigation in check 5, `browser_network_requests` shows no `/api/` request made after the last `POST .../items`: no `GET /api/basket/...` and no `/api/catalog` request.
8. **Direct load:** `browser_navigate` to `<web>/cart` (a full reload). The same two rows and total render. `browser_network_requests` for this load shows exactly one `GET <web>/api/basket/basket/<B>` → 200 (from `App.vue`) and no `/api/catalog` or `/api/ordering` request. `browser_console_messages` has no errors or warnings from the app.
9. **Loading skeleton:** delay the basket GET with `browser_run_code_unsafe` → `await page.route('**/api/basket/**', async r => { await new Promise(x => setTimeout(x, 3000)); await r.continue(); })`, then `browser_navigate` to `<web>/cart`. Within the first 3 s, `browser_evaluate` → `!!document.querySelector('main .e-card') && document.querySelector('.cart-table') === null && !document.body.innerText.includes('Your cart is empty')` is `true` (the `ECard` skeleton, with no table and no empty state). After the delay, the table replaces it. Then remove the delay with `page.unroute('**/api/basket/**')`.
10. **Error:** stop `basket` in the dashboard and `browser_navigate` to `<web>/cart`. Within 15 s, the snapshot shows an `alert` with `Could not load the cart` and the detail `HTTP 502 Bad Gateway`. The page shows only the `Cart` heading: no table, no `Your cart is empty`, and no skeleton (`browser_evaluate` → `!document.querySelector('main .e-card') && document.querySelector('.cart-table') === null && !document.body.innerText.includes('Your cart is empty')` is `true`). Start `basket` again, reload `/cart`, and the two rows return.
11. **Scope fence:** `git diff --stat main...HEAD` lists only the two files in Files touched plus files under `_agenticdev/`. `git diff main...HEAD -- src/apphost.mts src/vue-front/storefront/vite.config.ts src/vue-front/storefront/package.json src/vue-front/storefront/src/services` is empty.

## References
- PROJECT.md §6 Storefront (`/cart` shows the lines and the total and calls basket `GET`; the +/- buttons, remove and **Checkout** come later), §4.2 Basket (`GET /basket/{buyerId}` → `{ buyerId, items[], total }`, `total` is computed by Basket, and the price shown in the basket is the price charged), §3 Identity (`buyerId`), §2 Architecture (the browser talks only to the storefront origin, and errors propagate to the UI), §9 out of scope, §10 journey step 3 (viewing part).
- PLAN.md Phase 5 → Storefront, spec 8 (`08-cart-view.md`), which replaces part of the old 5.3.
