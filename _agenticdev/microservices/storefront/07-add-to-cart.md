# storefront 07 — add to cart

**Status:** Done (bd97a18)

## Goal
Let the buyer add a product from its card on `/`. Basket looks the price up itself, so the page sends only the product id and a quantity of 1. The basket that the POST returns becomes the shared basket state, so the header badge updates with no extra request. This completes the "add two different products, and add one of them twice" step of the journey (PROJECT.md §10).

## Context
- storefront/06 is merged (ca76da0). `src/vue-front/storefront/src/views/HomeView.vue` renders the grid as `<ul class="product-grid">` with one `<li class="product-card" data-testid="product-card">` per product. Each card is an `<ECard>` holding the name (`h2.product-card__name`), category, optional description and price (`p.product-card__price`, `data-testid="product-price"`, `formatPrice(product.price)`). The cards have no button. The page also has the category select, the search form and the pager. None of those change here.
- storefront/02 is merged (049f4be). The basket wiring it added:
  - `src/stores/basket.ts`: a module-level `state = ref<Basket | null>(null)`, exported as `basket` (readonly). `itemCount` is the sum of `quantity`. `refreshBasket()` GETs the basket, and on failure sets `null` and calls `showError('Could not load the cart', error)`; it never rejects. `useBasket()` returns `{ basket, itemCount, refreshBasket }`. The `BasketService` is created lazily (`service ??= new BasketService()`), because `ApiService` reads `Core.baseURL` and that is set only after `Core.init`.
  - `src/services/BasketService.ts`: the `Basket` / `BasketItem` interfaces and `class BasketService extends ApiService` (axios base URL `/api/basket/basket`) with only `get(buyerId)`.
  - `src/components/AppHeader.vue` shows `itemCount` as the badge (`data-testid="cart-badge"`, aria-label `Cart, N items`), so updating `state` is enough for the badge to change.
  - `src/errors.ts`: `showError(message, error)` shows a sticky error toast whose detail is `HTTP <status> <statusText>`.
  - The AppHost already gives the storefront `services__basket__http__0` + `waitFor(basket)`, and `vite.config.ts` already proxies `/api/basket` with a 10 s `proxyTimeout`. Nothing in either changes.
- basket/02 is merged (64a461a). `POST /basket/{buyerId}/items` `{ productId, quantity }` validates the request, looks the product up in Catalog (404 if unknown), appends the line or adds to its quantity, and returns **200** with the whole basket in the same shape as `GET`. The trace is basket → catalog `GET /products/{id}` → Postgres.
- What vue-base `v0.0.1` ships for the button, read from `node_modules/@guilherme/vue-base/dist/my-vue-core.js`: `EButton` has the props `label`, `prependIcon`, `loading` (shows a spinner and `loadingLabel`, and disables the button), `loadingLabel` (default `"Loading"`), `disabled` and `nativeType`. It emits `click` only when it is neither `disabled` nor `loading`.
- `ApiService` exposes the axios instance as `this.request`, whose base URL is `` `${baseURL}/${resource}` ``. So `this.request.post(`${buyerId}/items`, body)` sends `POST /api/basket/basket/<buyerId>/items`.

## Depends on
- `storefront/02-cart-badge`
- `storefront/03-product-grid`
- `basket/02-add-item`

## Scope
- [ ] `BasketService.addItem(buyerId, productId)` → `POST /basket/{buyerId}/items` with the body `{ productId, quantity: 1 }`, returning the basket.
- [ ] Store `addItem(productId)`: on success the returned basket becomes the shared state (the badge updates). On failure it shows `showError('Could not add to cart', error)` and leaves the state unchanged. It never rejects.
- [ ] **Add to cart** button on each card. While an add runs, the clicked card's button shows loading and every other card's button is disabled.

## Explicit non-goals
- No quantity picker on the card. No AppHost change (the basket reference came with storefront/02), and no `vite.config.ts` change.
- No success toast: the badge change is the feedback (decided). No `refreshBasket()` or extra `GET` after an add, whether it succeeds or fails (decided).
- Prices come only from what Basket and Ordering return. No page asks Catalog for a price again, and no request body sends a price to any service (adding sends only `productId` and `quantity`).
- Checkout is one call to Basket (`POST /basket/{buyerId}/checkout`). No page calls Ordering to create anything (no `POST /orders`).
- No page reads a database. The browser talks only to the storefront origin, through `/api/{service}`.
- No changes to catalog, basket, ordering, postgres, redis or their AppHost registrations.
- No AppHost reference to a service this spec's pages don't call.
- Nothing from PROJECT.md §9: no login or auth, payments, shipping, product admin or images, stock, order status, checkout form, re-pricing, retries or fallbacks, order paging, or frontend tests.

## Data model
None on the server, and no new browser storage. `eshop.buyerId` is still read only through `getBuyerId()`.

**Request** sent by the storefront: `POST /api/basket/basket/{buyerId}/items` with the JSON body `{"productId":<id>,"quantity":1}`. It has no other fields.

**Response read:** `200` → `Basket` (unchanged interface: `{ buyerId, items: BasketItem[], total }`).

**Store** (`src/stores/basket.ts`), one new export:

| Export | Type | Meaning |
|---|---|---|
| `addItem(productId)` | `(productId: number) => Promise<void>` | POST one unit for `getBuyerId()`. On success `state.value = <returned basket>`. On failure `showError('Could not add to cart', error)` and `state` is untouched. Never rejects. |

`useBasket()` returns `{ basket, itemCount, refreshBasket, addItem }`.

**Page state** added to `HomeView.vue`:

| Name | Type | Meaning |
|---|---|---|
| `addingId` | `Ref<number \| null>` | Id of the product whose add is in flight, or `null`. The button of that card has `loading`, and every other card's button is `disabled` while it is not `null`. |

## Tasks
1. **Basket client** (`src/services/BasketService.ts`): add `async addItem(buyerId: string, productId: number): Promise<Basket>`, which returns `(await this.request.post(`${buyerId}/items`, { productId, quantity: 1 })).data`. Typed the same way as `get` (`const response: { data: Basket } = …`). No other change.
2. **Store** (`src/stores/basket.ts`): add `export async function addItem(productId: number): Promise<void>`. It uses `service ??= new BasketService()`, then `try { state.value = await service.addItem(getBuyerId(), productId); } catch (error) { showError('Could not add to cart', error); }`. Add `addItem` to `useBasket()`'s return. Update the top comment so it says pages that change the basket either set it from the response (`addItem`) or call `refreshBasket()`.
3. **Page logic** (`src/views/HomeView.vue`): `const { addItem } = useBasket();`, `const addingId = ref<number | null>(null);` and
   `async function onAdd(productId: number): Promise<void> { if (addingId.value !== null) return; addingId.value = productId; try { await addItem(productId); } finally { addingId.value = null; } }`.
4. **Template** (`HomeView.vue`): inside each card's `<ECard>`, after the price, add `<EButton class="product-card__add" label="Add to cart" prepend-icon="add_shopping_cart" loading-label="Adding…" :loading="addingId === product.id" :disabled="addingId !== null && addingId !== product.id" data-testid="add-to-cart" @click="onAdd(product.id)" />`. Use `void onAdd(...)` in the handler if the lint rules require it. Scoped style: `.product-card__add { margin-top: 0.75rem; }`, and the price paragraph keeps its style. Filters, skeleton, empty state and pager stay as they are.

## Files touched
- `src/vue-front/storefront/src/services/BasketService.ts` (modified)
- `src/vue-front/storefront/src/stores/basket.ts` (modified)
- `src/vue-front/storefront/src/views/HomeView.vue` (modified)

No AppHost, `vite.config.ts` or dependency changes (`package.json` and `package-lock.json` are untouched). Nothing in catalog, basket or ordering changes.

## Definition of Done
Run every check from the repository root unless it says otherwise. `<web>` is the storefront URL from the dashboard (storefront → Endpoints). `B` is the buyerId, read in the browser with the Playwright MCP `browser_evaluate` → `localStorage.getItem('eshop.buyerId')`. In Git Bash, set `B=<that value>`. Browser checks use the Playwright MCP (`browser_navigate`, `browser_snapshot`, `browser_click`, `browser_console_messages`, `browser_network_requests`, `browser_evaluate`).

1. **AppHost build:** in `src/`, `npm run build` exits 0 with no eslint or tsc errors.
2. **Storefront build:** in `src/vue-front/storefront`, `npm ci && npm run build` exits 0, `vue-tsc` reports no errors, and `dist/index.html` exists. Since no Java service is touched, there's no `mvn test`.
3. **Healthy start:** in `src/`, `aspire run` shows every resource (postgres, catalogdb, orderingdb, pgAdmin, redis, catalog, basket, ordering, storefront) as Running/Healthy.
4. **Buttons:** on a fresh buyer (`browser_evaluate` → `localStorage.clear()`, then reload), `browser_navigate` to `<web>/`. The snapshot shows one enabled `Add to cart` button in each product card (`browser_evaluate` → `document.querySelectorAll('[data-testid="product-card"] [data-testid="add-to-cart"]').length` equals the number of cards, 8). There is no cart badge.
5. **Add:** `browser_click` **Add to cart** on the first card, then on the second card, then on the first card again, waiting for each to settle (the button is enabled again). The snapshot shows the cart link named `Cart, 3 items`, and `browser_evaluate` → `document.querySelector('[data-testid="cart-badge"]').textContent.trim()` is `"3"`. No alert is shown, and `browser_console_messages` has no errors or warnings from the app.
6. **Request body:** `browser_network_requests` shows exactly three `POST <web>/api/basket/basket/<B>/items` → 200 for the three clicks. Each body (`browser_network_request` on it) is `{"productId":<id>,"quantity":1}`, with the first and second card's ids and no other field (no price). The clicks add no `GET /api/basket/...` request and no `/api/catalog` request, so the only `/api/` requests after the initial page load are those three POSTs.
7. **Snapshot:** `curl -s <web>/api/basket/basket/$B` returns two lines: the first card's product with `quantity` 2 and the second card's with `quantity` 1. Each line's `productName` and `unitPrice` match the name and the price shown on its card (`$19.90` ↔ `19.9`), and `total` equals 2 × the first price + the second price.
8. **In flight:** run `browser_evaluate` → `document.querySelector('[data-testid="product-card"] [data-testid="add-to-cart"]').click(); const b = [...document.querySelectorAll('[data-testid="add-to-cart"]')]; await new Promise(r => setTimeout(r)); return b.map(x => x.disabled)`. Every value is `true` (the clicked one is loading and the others are disabled). Then wait for it to settle. The badge shows `4`, and all buttons are enabled again.
9. **Error:** stop `basket` in the dashboard and click **Add to cart** on any card. Within 15 s, the snapshot shows an `alert` with `Could not add to cart` and the detail `HTTP 502 Bad Gateway`. The badge still shows `4`, every button is enabled again, and `browser_network_requests` shows the failed POST and no `GET /api/basket/...` after it. Start `basket` again, reload, and the badge shows `4`.
10. **Trace:** dashboard → Traces has a trace for basket `POST /basket/{buyerId}/items` with child spans for catalog `GET /products/{id}` and its Postgres query.
11. **Scope fence:** `git diff --stat main...HEAD` lists only the three files in Files touched plus files under `_agenticdev/`. `git diff main...HEAD -- src/apphost.mts src/vue-front/storefront/vite.config.ts src/vue-front/storefront/package.json` is empty, and `grep -rn "price" src/vue-front/storefront/src/services/BasketService.ts` shows no price in any request body (only the `unitPrice` interface field).

## References
- PROJECT.md §3 Identity without login (`buyerId`), §4.2 Basket (`POST /basket/{buyerId}/items` `{ productId, quantity=1 }`, Basket looks the price up, the price shown in the basket is the price charged), §2 Architecture (browser talks only to the storefront origin, errors propagate to the UI, no retries), §6 Storefront (`/` has **Add to cart** on each card and calls basket `POST /items`; the badge is the sum of quantities), §9 out of scope, §10 journey step 2.
- PLAN.md Phase 5 → Storefront, spec 7 (`07-add-to-cart.md`), which replaces part of the old 5.2.
