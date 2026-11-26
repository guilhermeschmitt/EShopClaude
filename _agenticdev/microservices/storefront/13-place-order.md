# storefront 13 — place order

**Status:** Not started

## Goal
Let the buyer place the order from `/checkout` with one call to Basket, then land on the new order. Disabling the button while the call runs and the failure path stay in this spec, because both are checked on that same single call.

## Context
- storefront/12 is merged (5f99e8f). `src/vue-front/storefront/src/views/OrderDetailView.vue` shows `/orders/:id` (heading `Order #<id>`, a `Placed …` line, an `Order lines` table with `order-line` rows and `order-total`), loading it with one `GET /api/ordering/orders/{id}` on mount.
- `src/views/CheckoutView.vue` (storefront/10) is a read-only review. It takes `{ basket, status, refreshBasket }` from `useBasket()`, computes `items` and `total`, and runs `void refreshBasket()` once on mount. After `<h1>Checkout</h1>` it renders `<ECard loading />` while loading, nothing when failed, `EEmptyState` "Your cart is empty" + a `.checkout-back` **Back to cart** link when empty, and otherwise an `ECard` with `<table class="cart-table" aria-label="Order review">` (`review-line` rows, `line-quantity`, `line-total`, `tfoot` `review-total`). The page has no button today. Its top comment says "storefront/13 adds Place order".
- `src/stores/basket.ts`: `useBasket()` → `{ basket, itemCount, status, refreshBasket, addItem, updateQuantity }`. `refreshBasket()` GETs `/basket/{buyerId}`, never rejects, and on failure sets `status = 'failed'` with the toast `Could not load the cart`. `itemCount` drives the header badge.
- `src/components/AppHeader.vue`: the cart link's `aria-label` is `Cart, <n> items` when `itemCount > 0` and `Cart` otherwise. `<span data-testid="cart-badge">` is rendered only when `itemCount > 0`, so "badge 0" means no `cart-badge` element and the label `Cart`.
- `src/services/BasketService.ts`: `BasketService extends ApiService`, `config()` → `{ baseURL: \`${Core.baseURL}/basket\`, resource: 'basket' }`, with `get`, `addItem` (`this.request.post(\`${buyerId}/items\`, …)`) and `updateQuantity`. Construct only after `Core.init`.
- `src/errors.ts` `showError(message, error)` shows a sticky vue-base error toast (an `alert`) with the detail `HTTP <status> <text>`, or the error message for a network failure. `src/buyer.ts` `getBuyerId()` reads `eshop.buyerId`.
- `CartView.vue` and `HomeView.vue` show the patterns: a one-at-a-time ref (`updatingId`, `addingId`) with an early return in the handler, `EButton` with `label`, `type="primary"`, `prepend-icon`, `:loading` + `loading-label`, `:disabled`, and the right-aligned `.cart-actions` wrapper.
- The AppHost already gives the storefront `services__basket__http__0` and `services__ordering__http__0` with their `waitFor`s, and `vite.config.ts` already proxies `/api/basket` and `/api/ordering` (storefront/02 and 11). **Neither changes here.**
- Basket (basket/04, Done 5ee7850): `POST /basket/{buyerId}/checkout` → **201** `{ "orderId": <id> }` and the basket key is deleted. A missing or empty basket → **400**. Any Ordering failure (down, 4xx, 5xx) → **500**, and the basket is left exactly as it was.

## Depends on
- `storefront/10-checkout-review`
- `storefront/12-order-detail`
- `basket/04-checkout`

## Scope
- [ ] `BasketService.checkout(buyerId)` (decided: service + view, the store is unchanged) → `POST /basket/{buyerId}/checkout` with no body, returning `orderId`.
- [ ] **Place order** on `/checkout` (decided): a primary `EButton` in a right-aligned `.checkout-actions` below the review card, rendered only when the review table is (status `ready` with lines). While the POST runs it shows `:loading` with `loading-label="Placing order…"` and is disabled, and the handler returns early if a POST is already in flight, so a double-click sends one request.
- [ ] On 201 `{ orderId }` (decided: navigate, then refresh): `router.push('/orders/<orderId>')`, then `void refreshBasket()`. That one GET brings the badge to 0 from Basket's own state, and `/checkout` never flashes the empty state.
- [ ] On any failure (decided: one toast, stay): 5xx, network or 400 all show the sticky toast `Could not place the order`. The page stays on `/checkout` with the review unchanged, sends no re-GET, and the button is usable again for a retry. Basket keeps the basket (basket/04), so the badge doesn't change.

## Explicit non-goals
- No client-side retry, no idempotency key and no direct call to Ordering from the page.
- No new store action and no change to `basket.ts` (decided). No local clearing of the basket state: the badge drops to 0 only through `refreshBasket()`.
- No special handling of a 400: it gets the same toast as any other failure, with no refresh and no redirect (decided).
- No change to `/cart`, `/orders`, `/orders/:id`, `App.vue`, `AppHeader.vue`, the router, `errors.ts`, `format.ts`, `OrderingService.ts`, the other services, `vite.config.ts`, the AppHost or the dependencies.
- Prices come only from what Basket and Ordering return. No page asks Catalog for a price again, and no request body sends a price to any service (adding sends only `productId` and `quantity`).
- Checkout is one call to Basket (`POST /basket/{buyerId}/checkout`). No page calls Ordering to create anything (no `POST /orders`).
- No page reads a database. The browser talks only to the storefront origin, through `/api/{service}`.
- No changes to catalog, basket, ordering, postgres, redis or their AppHost registrations.
- No AppHost reference to a service this spec's pages don't call.
- Nothing from PROJECT.md §9: no login or auth, payments, shipping, product admin or images, stock, order status, checkout form, re-pricing, retries or fallbacks, order paging, or frontend tests.

## Data model
None on the server, and no new browser storage. `eshop.buyerId` is still read only through `getBuyerId()`.

**Requests sent by `/checkout`:** the existing `GET /api/basket/basket/{buyerId}` on mount, plus, per **Place order** click while none is in flight, one `POST /api/basket/basket/{buyerId}/checkout` with no body (the proxy strips `/api/basket`, so Basket sees `POST /basket/{buyerId}/checkout`). After a 201, the store's `GET /api/basket/basket/{buyerId}` and `/orders/:id`'s `GET /api/ordering/orders/{orderId}` follow. No price, item or `POST /orders` is ever sent.

**Response read** (type added to `src/services/BasketService.ts`):

```ts
/** `POST /basket/{buyerId}/checkout` → 201: the id of the order Ordering created. */
export interface CheckoutResult {
  orderId: number;
}
```

**`BasketService.checkout(buyerId: string): Promise<number>`** → `this.request.post(\`${buyerId}/checkout\`)` and returns `response.data.orderId`. An empty basket → axios rejects with `response.status` 400. Ordering down → 500.

**`/checkout` additions** (`CheckoutView.vue`):

| Element | Markup |
|---|---|
| Actions | `<div v-if="status === 'ready' && items.length > 0" class="checkout-actions">`, after the review `ECard` |
| Place order button | `<EButton label="Place order" type="primary" prepend-icon="shopping_bag" loading-label="Placing order…" :loading="placing" :disabled="placing" data-testid="place-order" @click="onPlaceOrder" />` |

**Script state:** `placing = ref(false)`. `onPlaceOrder()`:

```ts
if (placing.value) return;
placing.value = true;
try {
  const orderId = await service.checkout(getBuyerId());
  await router.push(`/orders/${orderId}`);
  void refreshBasket();
} catch (error) {
  showError('Could not place the order', error);
} finally {
  placing.value = false;
}
```

`service` is a `new BasketService()` created in `setup` (after `Core.init`, as `HomeView` does). In practice only the POST can reach the `catch`: `refreshBasket()` never rejects, and the push to `/orders/<id>` has no guard that could fail it.

## Tasks
1. **Service** (`src/vue-front/storefront/src/services/BasketService.ts`, modified): add the `CheckoutResult` interface and `checkout(buyerId)` as in Data model, with a doc comment in the existing style (no body sent; Basket creates the order through Ordering and clears the basket only after it exists; 400 when empty). `get`, `addItem` and `updateQuantity` don't change.
2. **Checkout script** (`src/vue-front/storefront/src/views/CheckoutView.vue`, modified): import `ref`, `useRouter`, `getBuyerId` from `@/buyer`, `showError` from `@/errors` and `BasketService`. Create `router` and `service`, add `placing` and `onPlaceOrder` as in Data model. Replace "storefront/13 adds Place order" in the top comment with: Place order POSTs the checkout once (disabled while it runs), then opens the new order and refreshes the badge; a failure shows a toast and keeps the page as it is.
3. **Checkout template:** after the review `ECard`, add the `.checkout-actions` block with the **Place order** button from Data model. The loading, failed and empty branches and the table don't change.
4. **Checkout styles** (scoped): `.checkout-actions { display: flex; justify-content: flex-end; margin-top: 1rem; }`, as `/cart`'s `.cart-actions`.

## Files touched
- `src/vue-front/storefront/src/services/BasketService.ts` (modified)
- `src/vue-front/storefront/src/views/CheckoutView.vue` (modified)

Nothing else changes: not `src/apphost.mts`, `vite.config.ts`, `basket.ts`, `buyer.ts`, `errors.ts`, `format.ts`, the router, `App.vue`, the components, the other views or services, `package.json` / `package-lock.json`, and nothing in catalog, basket or ordering.

## Definition of Done
Run every check from the repository root unless it says otherwise.
- `<web>` is the storefront URL from the dashboard (storefront → Endpoints).
- `B` is the buyerId, read with the Playwright MCP `browser_evaluate` → `localStorage.getItem('eshop.buyerId')`. In Git Bash, set `B=<that value>`.
- Browser checks use the Playwright MCP (`browser_navigate`, `browser_snapshot`, `browser_click`, `browser_console_messages`, `browser_network_requests`, `browser_evaluate`, `browser_run_code_unsafe`).
- **Badge 0** means `document.querySelector('[data-testid="cart-badge"]')` is `null` and the header cart link is named `Cart`.
- **Order count** is `curl -s "<web>/api/ordering/orders?buyerId=$B" | grep -o '"id":' | wc -l` (each `OrderSummary` has one `id`).
- **Fill** means: `browser_navigate` to `<web>/`, click **Add to cart** on the first card twice and on the second card once, waiting for each to settle.

1. **AppHost build:** in `src/`, `npm run build` exits 0 with no eslint or tsc errors.
2. **Storefront build:** in `src/vue-front/storefront`, `npm ci && npm run build` exits 0, `vue-tsc` reports no errors, and `dist/index.html` exists. No Java service is touched, so there's no `mvn test`.
3. **Healthy start:** in `src/`, `aspire run` shows every resource (postgres, catalogdb, orderingdb, pgAdmin, redis, catalog, basket, ordering, storefront) as Running/Healthy.
4. **No button when empty:** `browser_navigate` to `<web>/`, `browser_evaluate` → `localStorage.clear()`, `browser_navigate` to `<web>/checkout`. The snapshot shows `Your cart is empty` and no **Place order** button; `document.querySelector('[data-testid="place-order"]')` is `null`.
5. **Journey:** read `B`. Fill. Click the header cart link, then **Checkout**. On `/checkout`, note the two `review-line` rows (names, unit prices, quantities `2` and `1`, line totals) and `review-total`, and see an enabled **Place order** button. Clear the network log by reading it with `browser_network_requests`. Click **Place order**. Within 10 s, the URL is `<web>/orders/<id>`, the heading is `Order #<id>`, the `Order lines` table has two `order-line` rows whose names, `line-unit-price`, `line-quantity` and `line-total` equal the review's, in the same order, `order-total` equals `review-total`, and the header shows Badge 0. Order count is `1`.
6. **Network:** the requests logged since the click in check 5 are exactly: one `POST <web>/api/basket/basket/<B>/checkout` → 201, one `GET <web>/api/basket/basket/<B>` → 200 and one `GET <web>/api/ordering/orders/<id>` → 200. There is no `POST` to `/api/ordering` and no `/api/catalog` request. `curl -s <web>/api/basket/basket/$B` returns `"items":[]`.
7. **Disabled while running:** Fill, then `browser_navigate` to `<web>/checkout`. Note the Order count as `N`. Delay the POST with `browser_run_code_unsafe` → `await page.route('**/api/basket/basket/*/checkout', async r => { await new Promise(x => setTimeout(x, 3000)); await r.continue(); })`. Clear the network log. Run `browser_run_code_unsafe` → `await page.getByTestId('place-order').dblclick()`. Within the first 3 s, `document.querySelector('[data-testid="place-order"]').disabled` is `true` and the snapshot shows `Placing order…`. After the delay, the page lands on `<web>/orders/<id>`. The network log has exactly one `POST …/api/basket/basket/<B>/checkout`, and the Order count is `N + 1`. Remove the delay with `page.unroute('**/api/basket/basket/*/checkout')`.
8. **Failure keeps the basket:** Fill and note the header cart link's name (`Cart, 3 items`). `browser_navigate` to `<web>/checkout` and note `review-total`. Save `curl -s <web>/api/basket/basket/$B` as `before`, and the Order count as `N`. Stop `ordering` in the dashboard. Clear the network log, then click **Place order**. Within 15 s:
   - the snapshot shows an `alert` with `Could not place the order` and a detail starting with `HTTP 5`;
   - the URL is still `<web>/checkout`, the `Order review` table still shows the two rows and the same `review-total`, and `place-order` `.disabled` is `false`;
   - the network log has one `POST …/checkout` → 5xx and no `GET …/api/basket/basket/<B>` after it;
   - the header cart link is still named `Cart, 3 items`, and `curl -s <web>/api/basket/basket/$B` equals `before`.

   Start `ordering` and wait until it is Healthy. Close the toast and click **Place order** again. The page lands on `<web>/orders/<id>` with the two lines, the header shows Badge 0, and the Order count is `N + 1`.
9. **Empty at click time:** Fill, `browser_navigate` to `<web>/checkout`, then empty the basket outside the page: `curl -s -X PUT -H 'Content-Type: application/json' -d '{"quantity":0}' <web>/api/basket/basket/$B/items/<id>` → 200 for each of the two product ids. Click **Place order**. The snapshot shows an `alert` with `Could not place the order` and the detail `HTTP 400`, the URL is still `<web>/checkout`, and `place-order` `.disabled` is `false`.
10. **Trace:** dashboard → Traces shows, for the checkout in check 5, one trace that contains basket's `POST /basket/{buyerId}/checkout` server span, basket's HTTP client `POST` to ordering, ordering's `POST /orders` server span and ordering's Postgres `INSERT` span(s) on `orderingdb`. There is no catalog span in it. Check by hand or with a Playwright MCP snapshot of the trace detail.
11. **Console:** across checks 4–7, `browser_console_messages` has no errors or warnings from the app. The browser's own failed-resource messages for the 5xx in check 8 and the 400 in check 9 are expected.
12. **Scope fence:** `git diff --stat main...HEAD` lists only the two files in Files touched, plus files under `_agenticdev/`. `git diff main...HEAD -- src/apphost.mts src/java-back src/vue-front/storefront/vite.config.ts src/vue-front/storefront/package.json src/vue-front/storefront/package-lock.json src/vue-front/storefront/src/App.vue src/vue-front/storefront/src/main.ts src/vue-front/storefront/src/buyer.ts src/vue-front/storefront/src/components src/vue-front/storefront/src/router src/vue-front/storefront/src/stores src/vue-front/storefront/src/format.ts src/vue-front/storefront/src/errors.ts src/vue-front/storefront/src/services/OrderingService.ts src/vue-front/storefront/src/services/CatalogService.ts src/vue-front/storefront/src/services/CategoryService.ts src/vue-front/storefront/src/views/HomeView.vue src/vue-front/storefront/src/views/CartView.vue src/vue-front/storefront/src/views/OrdersView.vue src/vue-front/storefront/src/views/OrderDetailView.vue` is empty.

## References
- PROJECT.md:
  - §5 Checkout flow, steps 1 (Place order disabled while the request runs, one `POST /api/basket/basket/{buyerId}/checkout`) and 6 (refresh the badge, navigate to `/orders/{orderId}`), and the failure paragraph (the basket stays intact and the user can retry);
  - §4.2 Basket: `POST /basket/{buyerId}/checkout` → 201 `{ orderId }`, or 400 if empty;
  - §6 Storefront: `/checkout` calls basket `GET` and `POST /checkout`;
  - §2 Architecture (browser → storefront origin only; errors propagate to the UI; the checkout trace is the showcase);
  - §9 (no retries, no checkout form, no frontend tests);
  - §10 journey step 4 (order detail with snapshot prices, correct total, badge 0) and the checkout trace.
- PLAN.md Phase 5 → Storefront, spec 13 (`13-place-order.md`), the place-order half of the old 5.4.
- Patterns: `microservices/storefront/10-checkout-review.md` (the `/checkout` page), `microservices/storefront/12-order-detail.md` (the landing page and DoD style), `microservices/storefront/09-cart-adjust.md` (one-at-a-time ref and disabled buttons) and `microservices/basket/04-checkout.md` (the endpoint's 201/400/500 contract).
