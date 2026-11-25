# storefront 09 — cart adjust

**Status:** Not started

## Goal
Let the buyer change quantities and remove lines on `/cart`. Remove stays in this spec because it is the same `PUT` with `quantity: 0`, so it can't be verified apart from +/-.

## Context
- storefront/08 is merged (4dfdc91). `src/vue-front/storefront/src/views/CartView.vue` renders, after `<h1>Cart</h1>`:
  - `<ECard loading />` while `status === 'loading'`;
  - nothing more when `status === 'failed'`;
  - `EEmptyState` "Your cart is empty" plus a **Browse products** link when the basket has no lines;
  - otherwise an `ECard` with `<table class="cart-table" aria-label="Cart lines">`: four columns (Product, Unit price, Quantity, Line total), one `<tr data-testid="cart-line">` per item keyed by `productId`, a `data-testid="line-total"` cell, and a `<tfoot>` "Total" row (`<th colspan="3">`) with `data-testid="cart-total"` showing Basket's `total`.
  - It sends no request of its own.
- `src/stores/basket.ts`:
  - module-level `state` (exported read-only as `basket`), `status` (`'loading' | 'ready' | 'failed'`) and `itemCount` (sum of quantities; the header badge in `AppHeader.vue` reads it);
  - `refreshBasket()` and `addItem(productId)` both never reject; `addItem` sets `state` from the POST response and `status` to `'ready'`, or on failure calls `showError('Could not add to cart', error)` and leaves the state alone;
  - `useBasket()` returns `{ basket, itemCount, status, refreshBasket, addItem }`.
- `src/services/BasketService.ts` (`extends ApiService`, `baseURL` `${Core.baseURL}/basket`, resource `basket`) has `get(buyerId)` and `addItem(buyerId, productId)`; `this.request` is vue-base's axios instance.
- `src/errors.ts` `showError(message, error)` shows a sticky error toast whose detail is `HTTP <status> <statusText>`. `src/format.ts` `formatPrice` formats `17` as `$17.00`.
- `HomeView.vue` (storefront/07) already uses a one-at-a-time pattern: a `ref` holding the id being added, every other button disabled until it settles.
- vue-base `EButton` (read from `node_modules/@guilherme/vue-base/dist/my-vue-core.js`): `icon` plus `label` renders an icon-only button whose `aria-label` is `label`; `size` (`small|medium|large`), `type` (`primary|secondary|success|danger`), `ghost`, `outlined` and `disabled` props; `loading` replaces the icon with a spinner plus the `loading-label` text (not used here). Icons are Material Symbols names (`add`, `remove`, `delete`).
- basket/03 is merged (6fe653c). `PUT /basket/{buyerId}/items/{productId}` with `{ quantity }` returns **200** with the basket in the same shape as `GET`. `quantity: 0` removes the line (an emptied basket is `items: []`, `total: 0`). A missing basket or line → 404. A negative or missing quantity → 400. The AppHost already gives the storefront `services__basket__http__0` + `waitFor(basket)`, and `vite.config.ts` already proxies `/api/basket` (storefront/02). Neither changes here.

## Depends on
- `storefront/08-cart-view`
- `basket/03-update-item-quantity`

## Scope
- [ ] `BasketService.updateQuantity(buyerId, productId, quantity)` → `PUT /basket/{buyerId}/items/{productId}` with the body `{ quantity }` only.
- [ ] Store `updateQuantity(productId, quantity)`: sets the shared state from the PUT response (decided), so lines, total and the header badge update from one request. Never rejects. On any failure (5xx, 404, network) it shows `showError('Could not update the cart', error)` and leaves the state as it was (decided: toast only, no refresh).
- [ ] `/cart`, per line: the Quantity cell becomes [−] N [+] with small icon-only `EButton`s; a new last column holds a remove button (decided).
  - **+** → `quantity + 1`. **−** → `quantity − 1`, so − at quantity 1 sends `0` and removes the line (it stays enabled at 1).
  - **Remove** → `quantity: 0`.
- [ ] One adjust at a time (decided): while a PUT is in flight, every +, − and remove button on the page is disabled and clicks are ignored. No spinner (decided).
- [ ] Removing the last line shows the storefront/08 empty state (`Your cart is empty`), with no extra code.

## Explicit non-goals
- No free-text quantity input. No optimistic update that shows numbers Basket didn't return. No `refreshBasket()` after a PUT, not even on a 404 (decided).
- No `EButton` `loading` state on these buttons (decided), no confirmation dialog on remove, no upper limit on quantity.
- No change to `App.vue`, `AppHeader.vue`, `HomeView.vue`, the router, the AppHost, `vite.config.ts` or the dependencies.
- Prices come only from what Basket and Ordering return. No page asks Catalog for a price again, and no request body sends a price to any service (adding sends only `productId` and `quantity`).
- Checkout is one call to Basket (`POST /basket/{buyerId}/checkout`). No page calls Ordering to create anything (no `POST /orders`).
- No page reads a database. The browser talks only to the storefront origin, through `/api/{service}`.
- No changes to catalog, basket, ordering, postgres, redis or their AppHost registrations.
- No AppHost reference to a service this spec's pages don't call.
- Nothing from PROJECT.md §9: no login or auth, payments, shipping, product admin or images, stock, order status, checkout form, re-pricing, retries or fallbacks, order paging, or frontend tests.

## Data model
None on the server, and no new browser storage. `eshop.buyerId` is still read only through `getBuyerId()`.

**Request sent:** `PUT /api/basket/basket/{buyerId}/items/{productId}` with the body `{ "quantity": N }`, where N ≥ 0. No price, name or other field.

**Response read:** the same `Basket = { buyerId, items: BasketItem[], total }` as `GET` (unchanged types in `BasketService.ts`).

**Service** (`src/services/BasketService.ts`), one new method:

| Method | Request | Returns |
|---|---|---|
| `updateQuantity(buyerId: string, productId: number, quantity: number): Promise<Basket>` | `this.request.put(\`${buyerId}/items/${productId}\`, { quantity })` | `response.data` |

**Store** (`src/stores/basket.ts`), one new export:

| Export | Behavior |
|---|---|
| `updateQuantity(productId: number, quantity: number): Promise<void>` | On success, `state` becomes the PUT response and `status` becomes `'ready'`. On failure, `showError('Could not update the cart', error)` and `state`/`status` stay as they were. Never rejects. |

`useBasket()` returns `{ basket, itemCount, status, refreshBasket, addItem, updateQuantity }`.

**Page state** (`CartView.vue`): `const updatingId = ref<number | null>(null)` is the `productId` whose PUT is in flight. While it isn't `null`, every adjust button is `disabled` and `onAdjust` returns at once.

**What a line renders** (the table now has five columns):

| Column | Header | Cell |
|---|---|---|
| Product | `Product` | `productName` |
| Unit price | `Unit price` (right-aligned) | `formatPrice(unitPrice)` |
| Quantity | `Quantity` (right-aligned) | `<div class="cart-qty">`: `EButton icon="remove" size="small" outlined :label="\`Decrease quantity of ${productName}\`" data-testid="qty-decrease"`, `<span data-testid="line-quantity">{{ quantity }}</span>`, `EButton icon="add" size="small" outlined :label="\`Increase quantity of ${productName}\`" data-testid="qty-increase"` |
| Line total | `Line total` (right-aligned) | `formatPrice(unitPrice * quantity)` (`data-testid="line-total"`), display-only |
| Actions | `<span class="visually-hidden">Actions</span>` | `EButton icon="delete" size="small" type="danger" ghost :label="\`Remove ${productName}\`" data-testid="line-remove"` |

All three buttons get `:disabled="updatingId !== null"`. The `<tfoot>` row keeps `<th colspan="3">Total</th>` and the `cart-total` cell, plus one empty `<td>` for the Actions column.

## Tasks
1. **Service** (`src/services/BasketService.ts`): add `updateQuantity(...)` as in Data model, with a doc comment (`PUT`, `0` removes the line, no price sent).
2. **Store** (`src/stores/basket.ts`): add `updateQuantity(productId, quantity)` next to `addItem`, built the same way (`service ??= new BasketService()`, try/catch, `getBuyerId()`), with a doc comment. Add it to `useBasket()`'s return, and update the top comment (pages that change the basket set it from the response: `addItem`, `updateQuantity`).
3. **Page script** (`src/views/CartView.vue`): take `updateQuantity` from `useBasket()`; add `updatingId` and
   `async function onAdjust(productId: number, quantity: number)`: return if `updatingId.value !== null`; set `updatingId.value = productId`; `try { await updateQuantity(productId, quantity) } finally { updatingId.value = null }`. Update the top comment (this page now sends PUTs; it still never GETs).
4. **Page template:** follow "What a line renders". + calls `onAdjust(item.productId, item.quantity + 1)`, − calls `onAdjust(item.productId, item.quantity - 1)`, remove calls `onAdjust(item.productId, 0)`. The loading, failed and empty branches don't change.
5. **Styles** (scoped): `.cart-qty { display: inline-flex; align-items: center; gap: 0.5rem; }`, a `min-width` of about `2ch` with centered text on the quantity span, `vertical-align: middle` on the cells, and a standard `.visually-hidden` rule (absolute, 1px clip, `white-space: nowrap`). The existing table rules stay.

## Files touched
- `src/vue-front/storefront/src/services/BasketService.ts` (modified)
- `src/vue-front/storefront/src/stores/basket.ts` (modified)
- `src/vue-front/storefront/src/views/CartView.vue` (modified)

No AppHost, `vite.config.ts`, router, `App.vue`, `AppHeader.vue` or dependency changes (`package.json` and `package-lock.json` are untouched). Nothing in catalog, basket or ordering changes.

## Definition of Done
Run every check from the repository root unless it says otherwise.
- `<web>` is the storefront URL from the dashboard (storefront → Endpoints).
- `B` is the buyerId, read with the Playwright MCP `browser_evaluate` → `localStorage.getItem('eshop.buyerId')`. In Git Bash, set `B=<that value>`.
- Browser checks use the Playwright MCP (`browser_navigate`, `browser_snapshot`, `browser_click`, `browser_console_messages`, `browser_network_requests`, `browser_network_request`, `browser_evaluate`, `browser_run_code_unsafe`).
- **Match Basket** means that `curl -s <web>/api/basket/basket/$B` returns items and a `total` T where:
  - the `cart-line` rows show the same names and quantities in the same order;
  - each row's `line-total` is unitPrice × quantity in en-US dollars;
  - `[data-testid="cart-total"]` text equals T formatted as en-US dollars;
  - `[data-testid="cart-badge"]` text equals the sum of the quantities, or the badge is absent when the sum is 0.

1. **AppHost build:** in `src/`, `npm run build` exits 0 with no eslint or tsc errors.
2. **Storefront build:** in `src/vue-front/storefront`, `npm ci && npm run build` exits 0, `vue-tsc` reports no errors, and `dist/index.html` exists. No Java service is touched, so there's no `mvn test`.
3. **Healthy start:** in `src/`, `aspire run` shows every resource (postgres, catalogdb, orderingdb, pgAdmin, redis, catalog, basket, ordering, storefront) as Running/Healthy.
4. **Setup:** `browser_navigate` to `<web>/`, then `browser_evaluate` → `localStorage.clear()`, then `browser_navigate` to `<web>/` again. Click **Add to cart** on the first card twice and on the second card once, waiting for each to settle. Click the header cart link. The table shows two `cart-line` rows, with quantities `2` (first card, call it P1) and `1` (P2). The rows Match Basket.
5. **Increase:** click `Increase quantity of <P1>`. P1's `line-quantity` becomes `3`. The rows Match Basket, and the badge shows `4`. `browser_network_requests` shows exactly one new request, `PUT <web>/api/basket/basket/<B>/items/<P1 id>` → 200, and no `GET /api/basket/...` or `/api/catalog` request after it.
6. **Request body:** `browser_network_request` for that PUT shows the request body exactly `{"quantity":3}`: no `unitPrice`, `price`, `productName` or any other key.
7. **Decrease:** click `Decrease quantity of <P1>`. P1's quantity becomes `2`, the badge shows `3`, the rows Match Basket, and the PUT body is `{"quantity":2}`.
8. **Minus at 1 removes:** click `Decrease quantity of <P2>` (quantity 1). The PUT body is `{"quantity":0}` → 200. P2's row disappears, one `cart-line` row remains, and the rows Match Basket.
9. **Remove:** click `Remove <P1>`. The PUT body is `{"quantity":0}` → 200. The table is gone (`browser_evaluate` → `document.querySelector('.cart-table') === null` is `true`), the snapshot shows `Your cart is empty` and **Browse products**, the badge is absent (`document.querySelector('[data-testid="cart-badge"]') === null`), and `curl -s <web>/api/basket/basket/$B` returns `"items":[]` and a `total` of 0.
10. **One at a time:** add P1 again on `/` and go to `/cart` through the header link. Delay PUTs with `browser_run_code_unsafe` → `await page.route('**/api/basket/basket/*/items/*', async r => { if (r.request().method() === 'PUT') await new Promise(x => setTimeout(x, 3000)); await r.continue(); })`. Click `Increase quantity of <P1>`. Within the first 3 s:
    - `browser_evaluate` → `[...document.querySelectorAll('.cart-table button')].every(b => b.disabled)` is `true`;
    - a second click, sent as `browser_evaluate` → `document.querySelector('[data-testid="qty-increase"]').click()` (a `browser_click` would wait on the disabled button), sends no second PUT.

    After the delay, the quantity is `2` (not 3), the buttons are enabled again (`.every(b => !b.disabled)` is `true`), and `browser_network_requests` shows exactly one PUT for that click. Then remove the delay with `page.unroute('**/api/basket/basket/*/items/*')`.
11. **404 (stale line):** with P1 at quantity 2 on `/cart`, remove it outside the page: `curl -s -X PUT -H 'Content-Type: application/json' -d '{"quantity":0}' <web>/api/basket/basket/$B/items/<P1 id>` → 200. Click `Increase quantity of <P1>` on the page, which is now stale. The PUT returns 404, the snapshot shows an `alert` with `Could not update the cart` and a detail starting with `HTTP 404`, and P1's row still shows quantity `2` (the state is unchanged). No `GET /api/basket/...` follows the PUT. A reload of `/cart` then shows the empty state.
12. **5xx:** add P1 on `/`, open `/cart` through the header link, then stop `basket` in the dashboard. Click `Increase quantity of <P1>`. Within 15 s, the snapshot shows an `alert` with `Could not update the cart` and the detail `HTTP 502 Bad Gateway`, the row still shows quantity `1`, and the buttons are enabled again. Start `basket` again, click `Increase quantity of <P1>`, and the quantity becomes `2` and Matches Basket.
13. **Console:** across checks 4–10, `browser_console_messages` has no errors or warnings from the app (the failed requests in 11–12 are expected).
14. **Scope fence:** `git diff --stat main...HEAD` lists only the three files in Files touched, plus files under `_agenticdev/`. `git diff main...HEAD -- src/apphost.mts src/vue-front/storefront/vite.config.ts src/vue-front/storefront/package.json src/vue-front/storefront/package-lock.json src/vue-front/storefront/src/App.vue src/vue-front/storefront/src/components src/vue-front/storefront/src/router` is empty.

## References
- PROJECT.md:
  - §6 Storefront: `/cart` has lines with quantity +/- and remove, and calls basket `GET` and `PUT /items/{productId}`;
  - §4.2 Basket: `PUT /basket/{buyerId}/items/{productId}` `{ quantity }`, `0` removes the line, it returns the basket or 404, and the price shown is the price charged;
  - §3 Identity (`buyerId`);
  - §2 Architecture (the browser talks only to the storefront origin, and errors propagate to the UI);
  - §9 (no retries or fallbacks, no frontend tests);
  - §10 journey step 3 ("change a quantity in the cart and remove a line").
- PLAN.md Phase 5 → Storefront, spec 9 (`09-cart-adjust.md`), the rest of the old 5.3.
- Patterns: `microservices/storefront/07-add-to-cart.md` (store action set from the response, one at a time) and `microservices/storefront/08-cart-view.md` (the page and the DoD style).
