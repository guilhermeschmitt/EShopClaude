# storefront 10 — checkout review

**Status:** Not started

## Goal
Let the buyer move from the cart to a read-only review of what they are about to order. This gives storefront/13 the page that its **Place order** button goes on.

## Context
- storefront/09 is merged (236bafb). `src/vue-front/storefront/src/views/CartView.vue` renders, after `<h1>Cart</h1>`:
  - `<ECard loading />` while `status === 'loading'`;
  - nothing more when `status === 'failed'`;
  - `EEmptyState` "Your cart is empty" plus a **Browse products** link (`.cart-browse`) when the basket has no lines;
  - otherwise an `ECard` holding `<table class="cart-table" aria-label="Cart lines">` with five columns: Product, Unit price, Quantity (the − / + `EButton`s around `data-testid="line-quantity"`), Line total (`data-testid="line-total"`) and Actions (remove). There is one `<tr data-testid="cart-line">` per item, and a `<tfoot>` Total row with `data-testid="cart-total"`.
  - `updatingId` (`ref<number | null>`) is set while a PUT is in flight. The page never GETs.
- `src/stores/basket.ts`: `useBasket()` returns `{ basket, itemCount, status, refreshBasket, addItem, updateQuantity }`. `refreshBasket()` GETs `/basket/{buyerId}` and never rejects. On success it sets `state` and `status = 'ready'`. On failure it sets `state = null` and `status = 'failed'` and shows `showError('Could not load the cart', error)`. `status` never goes back to `'loading'` after the first load. `App.vue` calls `refreshBasket()` once on start.
- `src/views/CheckoutView.vue` is the storefront/01 placeholder: only `<h1>Checkout</h1>`. The router already maps `/checkout` → `CheckoutView` (name `checkout`).
- `src/format.ts` `formatPrice` formats `17` as `$17.00`.
- vue-base `EButton` (props as recorded in storefront/09 from `node_modules/@guilherme/vue-base/dist/my-vue-core.js`): `label`, `type` (`primary|secondary|success|danger`), `size`, `disabled`, `icon` (icon-only), `prepend-icon`. `HomeView.vue` already renders text buttons this way (`label="Add to cart" prepend-icon="add_shopping_cart"`, `label="Search" prepend-icon="search"`). `ECard` (with a `loading` prop) and `EEmptyState` (`icon`, `title`) are already used on `/cart`.
- The AppHost already gives the storefront `services__basket__http__0` + `waitFor(basket)`, and `vite.config.ts` already proxies `/api/basket` (storefront/02). Neither changes here.

## Depends on
- `storefront/08-cart-view`
- `basket/01-read-basket`

## Scope
- [ ] `/cart`: a primary **Checkout** `EButton` below the cart content (decided). It is rendered in both the empty and the filled state (not while loading or failed), and is disabled when the basket has no lines **or** while a +/−/remove PUT is in flight (`updatingId !== null`) (decided). A click runs `router.push('/checkout')`.
- [ ] `/checkout` calls `refreshBasket()` once on mount (decided), so the review shows Basket's current state. That's one `GET /basket/{buyerId}` per visit.
- [ ] `/checkout` renders from the shared store:
  - `<ECard loading />` while `status === 'loading'`;
  - nothing more when `status === 'failed'` (the store's `Could not load the cart` toast is the error);
  - `EEmptyState` "Your cart is empty" plus a **Back to cart** `RouterLink` to `/cart` when the basket has no lines (decided: no redirect);
  - otherwise a read-only table of lines plus the total, in its own markup in `CheckoutView.vue` (decided: no shared component). It has no +, −, remove or any other button.

## Explicit non-goals
- No **Place order** button and no `POST /checkout` (storefront/13). No editing on `/checkout`.
- No shared lines component, and no refactor of `/cart`'s table (decided).
- No redirect from an empty `/checkout` (decided). No new store action, no change to `BasketService.ts` or `basket.ts`.
- No change to `App.vue`, `AppHeader.vue`, `HomeView.vue`, the router, the AppHost, `vite.config.ts` or the dependencies.
- Prices come only from what Basket and Ordering return. No page asks Catalog for a price again, and no request body sends a price to any service (adding sends only `productId` and `quantity`).
- Checkout is one call to Basket (`POST /basket/{buyerId}/checkout`). No page calls Ordering to create anything (no `POST /orders`).
- No page reads a database. The browser talks only to the storefront origin, through `/api/{service}`.
- No changes to catalog, basket, ordering, postgres, redis or their AppHost registrations.
- No AppHost reference to a service this spec's pages don't call.
- Nothing from PROJECT.md §9: no login or auth, payments, shipping, product admin or images, stock, order status, checkout form, re-pricing, retries or fallbacks, order paging, or frontend tests.

## Data model
None on the server, and no new browser storage. `eshop.buyerId` is still read only through `getBuyerId()`.

**Request sent by `/checkout`:** `GET /api/basket/basket/{buyerId}` (through `refreshBasket()`), nothing else.

**Response read:** the unchanged `Basket = { buyerId, items: BasketItem[], total }`, with `BasketItem = { productId, productName, unitPrice, quantity }` (types in `BasketService.ts`).

**`/cart` addition** (`CartView.vue`):

| Element | Markup |
|---|---|
| Checkout button | `<div class="cart-actions"><EButton label="Checkout" type="primary" prepend-icon="shopping_cart_checkout" :disabled="items.length === 0 \|\| updatingId !== null" data-testid="checkout-button" @click="router.push('/checkout')" /></div>`, placed after the empty-state block and after the table card, rendered only when `status === 'ready'` |

**`/checkout` table** (`CheckoutView.vue`): `<table class="cart-table" aria-label="Order review">` inside an `ECard`, with four columns:

| Column | Header | Cell |
|---|---|---|
| Product | `Product` | `productName` |
| Unit price | `Unit price` (right-aligned) | `formatPrice(unitPrice)` |
| Quantity | `Quantity` (right-aligned) | `quantity` (`data-testid="line-quantity"`), plain text |
| Line total | `Line total` (right-aligned) | `formatPrice(unitPrice * quantity)` (`data-testid="line-total"`) |

There is one `<tr data-testid="review-line">` per item, keyed by `productId`. The `<tfoot>` row is `<th scope="row" colspan="3">Total</th>` plus `<td data-testid="review-total">{{ formatPrice(total) }}</td>`. There is no button inside the table or the page.

## Tasks
1. **Cart script** (`src/views/CartView.vue`): import `useRouter` from `vue-router` and create `const router = useRouter()`. Update the top comment (a Checkout button navigates to `/checkout`, disabled when empty or while a PUT runs).
2. **Cart template:** wrap the empty-state and table branches so that, when `status === 'ready'`, a `<div class="cart-actions">` with the Checkout `EButton` (as in Data model) follows them. The loading and failed branches render no button. The table, the adjust buttons and `onAdjust` don't change.
3. **Cart styles** (scoped): `.cart-actions { display: flex; justify-content: flex-end; margin-top: 1rem; }`.
4. **Checkout script** (`src/views/CheckoutView.vue`): `<script setup lang="ts">`. Take `{ basket, status, refreshBasket }` from `useBasket()`, with `items` and `total` computed as on `/cart`. In `onMounted`, run `void refreshBasket()`. Add a top comment (read-only review; one GET on mount through the store; storefront/13 adds Place order).
5. **Checkout template:** `<h1>Checkout</h1>`, then the four branches from Scope (loading, failed, empty with `EEmptyState icon="shopping_cart" title="Your cart is empty"` and `<RouterLink to="/cart" class="checkout-back">Back to cart</RouterLink>`, and the review table from Data model).
6. **Checkout styles** (scoped): copy `/cart`'s `.cart-table` rules (layout, borders, `.cart-table__num`, bold `tfoot`) and add `.checkout-back { display: inline-block; margin-top: 0.75rem; }`.

## Files touched
- `src/vue-front/storefront/src/views/CartView.vue` (modified)
- `src/vue-front/storefront/src/views/CheckoutView.vue` (modified)

No AppHost, `vite.config.ts`, router, store, service, `App.vue`, `AppHeader.vue` or dependency changes (`package.json` and `package-lock.json` are untouched). Nothing in catalog, basket or ordering changes.

## Definition of Done
Run every check from the repository root unless it says otherwise.
- `<web>` is the storefront URL from the dashboard (storefront → Endpoints).
- `B` is the buyerId, read with the Playwright MCP `browser_evaluate` → `localStorage.getItem('eshop.buyerId')`. In Git Bash, set `B=<that value>`.
- Browser checks use the Playwright MCP (`browser_navigate`, `browser_snapshot`, `browser_click`, `browser_console_messages`, `browser_network_requests`, `browser_evaluate`, `browser_run_code_unsafe`).
- **Match Basket** means that `curl -s <web>/api/basket/basket/$B` returns items and a `total` T where:
  - the `review-line` rows show the same names, unit prices and quantities in the same order;
  - each row's `line-total` is unitPrice × quantity in en-US dollars;
  - `[data-testid="review-total"]` text equals T formatted as en-US dollars.

1. **AppHost build:** in `src/`, `npm run build` exits 0 with no eslint or tsc errors.
2. **Storefront build:** in `src/vue-front/storefront`, `npm ci && npm run build` exits 0, `vue-tsc` reports no errors, and `dist/index.html` exists. No Java service is touched, so there's no `mvn test`.
3. **Healthy start:** in `src/`, `aspire run` shows every resource (postgres, catalogdb, orderingdb, pgAdmin, redis, catalog, basket, ordering, storefront) as Running/Healthy.
4. **Disabled when empty:** `browser_navigate` to `<web>/`, then `browser_evaluate` → `localStorage.clear()`, then `browser_navigate` to `<web>/cart`. The snapshot shows `Your cart is empty` and a **Checkout** button, and `browser_evaluate` → `document.querySelector('[data-testid="checkout-button"]').disabled` is `true`.
5. **Enabled with lines:** `browser_navigate` to `<web>/`. Click **Add to cart** on the first card twice and on the second card once, waiting for each to settle. Click the header cart link. Two `cart-line` rows show, and `document.querySelector('[data-testid="checkout-button"]').disabled` is `false`.
6. **Disabled during a PUT:** delay PUTs with `browser_run_code_unsafe` → `await page.route('**/api/basket/basket/*/items/*', async r => { if (r.request().method() === 'PUT') await new Promise(x => setTimeout(x, 3000)); await r.continue(); })`. Click `Increase quantity of <first product>`. Within the first 3 s, `checkout-button` `.disabled` is `true`. After the delay it is `false` again, and the first line's quantity is `3`. Remove the delay with `page.unroute('**/api/basket/basket/*/items/*')`. Then click `Decrease quantity of <first product>` so the quantities are `2` and `1` again.
7. **Review:** note the `/cart` names, quantities, line totals and `cart-total`. Click **Checkout**. The URL is `<web>/checkout`, the snapshot shows the heading `Checkout` and a table `Order review` with two `review-line` rows whose names, unit prices, quantities and line totals equal what `/cart` showed, and `review-total` equals `/cart`'s `cart-total`. The rows Match Basket.
8. **Read-only:** on `/checkout`, `browser_evaluate` → `document.querySelectorAll('main button').length` is `0`. The snapshot has no `Increase quantity`, `Decrease quantity`, `Remove`, `Place order` or `Checkout` button.
9. **Network:** clear the log by reading it with `browser_network_requests` on `/cart`, then click **Checkout**. The requests made after the click are exactly one `GET <web>/api/basket/basket/<B>` → 200, with no `PUT`, `POST`, `/api/catalog` or `/api/ordering` request.
10. **Fresh state:** with `/checkout` open, change the basket outside the page: `curl -s -X PUT -H 'Content-Type: application/json' -d '{"quantity":5}' <web>/api/basket/basket/$B/items/<second product id>` → 200. Click the header cart link, then **Checkout** again. The second row now shows quantity `5`, and the rows Match Basket.
11. **Empty review:** set both lines to 0 with the same `curl -X PUT ... '{"quantity":0}'` for each product id, then `browser_navigate` to `<web>/checkout`. The snapshot shows `Your cart is empty` and a **Back to cart** link, and there is no `Order review` table. Clicking **Back to cart** opens `<web>/cart`.
12. **Load failure:** add a product on `/`, then stop `basket` in the dashboard and `browser_navigate` to `<web>/checkout`. Within 15 s, the snapshot shows an `alert` with `Could not load the cart` and a detail starting with `HTTP 5`, and there is no `Order review` table and no empty state. Start `basket` again.
13. **Console:** across checks 4–11, `browser_console_messages` has no errors or warnings from the app (the failed request in 12 is expected).
14. **Scope fence:** `git diff --stat main...HEAD` lists only the two files in Files touched, plus files under `_agenticdev/`. `git diff main...HEAD -- src/apphost.mts src/vue-front/storefront/vite.config.ts src/vue-front/storefront/package.json src/vue-front/storefront/package-lock.json src/vue-front/storefront/src/App.vue src/vue-front/storefront/src/components src/vue-front/storefront/src/router src/vue-front/storefront/src/stores src/vue-front/storefront/src/services` is empty.

## References
- PROJECT.md:
  - §6 Storefront: `/cart` has a **Checkout** button, and `/checkout` is a read-only review of the lines and total that calls basket `GET` (`POST /checkout` belongs to storefront/13);
  - §4.2 Basket: `GET /basket/{buyerId}` returns `{ buyerId, items[], total }`, and the price shown is the price charged;
  - §5 Checkout flow, step 1 (the review page the Place order button goes on);
  - §3 Identity (`buyerId`);
  - §2 Architecture (the browser talks only to the storefront origin, and errors propagate to the UI);
  - §9 (no checkout form, no re-pricing, no frontend tests);
  - §10 journey step 4 (checkout).
- PLAN.md Phase 5 → Storefront, spec 10 (`10-checkout-review.md`), the review half of the old 5.4.
- Patterns: `microservices/storefront/08-cart-view.md` (the read-only table and its states) and `microservices/storefront/09-cart-adjust.md` (`updatingId`, the DoD style).
