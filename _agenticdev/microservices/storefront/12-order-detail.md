# storefront 12 — order detail

**Status:** Not started

## Goal
Show one order on `/orders/:id`: its lines with the snapshot prices, the total and the date. This is the page that storefront/13 lands on after **Place order**, and the target of every link on `/orders`.

## Context
- storefront/11 is merged (432f626). `src/vue-front/storefront/src/views/OrderDetailView.vue` is still the storefront/01 placeholder: `useRoute()` and `<h1>Order #{{ route.params.id }}</h1>`. The router already maps `/orders/:id` → `OrderDetailView` (name `order-detail`). `/orders` links each row to `/orders/<id>` (`data-testid="order-link"`).
- `src/apphost.mts` already gives the storefront `services__ordering__http__0` + `waitFor(ordering)`, and `vite.config.ts` already proxies `/api/ordering` with the prefix stripped (storefront/11). **Neither changes here.**
- `src/services/OrderingService.ts` (storefront/11) has `OrderSummary` and `OrderingService` (`extends ApiService`, `config()` → `{ baseURL: \`${Core.baseURL}/ordering\`, resource: 'orders' }`, `listByBuyer(buyerId)`). Services are constructed only after `Core.init`. `BasketService.get(buyerId)` → `this.request.get(buyerId)` is the pattern for a get by path segment.
- `src/errors.ts` `showError(message, error)` shows a sticky vue-base error toast with the detail `HTTP <status> <text>`. `src/format.ts` has `formatPrice` (en-US dollars) and `formatDate` (en-US, `dateStyle: 'medium'`, `timeStyle: 'short'`, browser timezone).
- `OrdersView.vue` loads once on mount with local `orders` / `loading` / `failed` refs. `CheckoutView.vue` shows the lines in a `.cart-table` (Product, Unit price, Quantity, Line total, with a `tfoot` Total row) inside an `ECard`, and has a `.checkout-back` `RouterLink`. Components in use: `ECard` (`loading`), `EEmptyState` (`icon`, `title`, `description`), `EButton`, `ESelect`, `EInputText`.
- Ordering (ordering/04, Done df164a2): `GET /orders/{id}` → 200 `Order = { id, buyerId, createdAt, total, items: [{ productId, productName, unitPrice, quantity }] }`, with the items in insertion order. `createdAt` is an ISO-8601 UTC string. An unknown id is 404 and a non-numeric id (`/orders/abc`) is 400, both with Spring's default error JSON.

## Depends on
- `storefront/11-my-orders`
- `ordering/04-get-order`

## Scope
- [ ] `OrderingService.get(id)` (decided, following `BasketService.get`) → `GET /orders/{id}`, plus the `Order` / `OrderItem` types.
- [ ] `/orders/:id` GETs once on mount with local `loading` / `notFound` / `failed` refs, as `OrdersView` does. There is no store and no watch on the route param.
- [ ] Layout like `/checkout` (decided): the h1 `Order #<id>`, a `Placed <formatDate(createdAt)>` line, then an `ECard` with a table (Product, Unit price, Quantity, Line total) and a `tfoot` **Total** row that shows Ordering's `total`. The lines are in Ordering's order. Line total = `unitPrice × quantity`, as on `/checkout`.
- [ ] The h1 is always `Order #<route id>` (decided), in every state.
- [ ] Not found (decided): a 404 **or** 400 response shows `EEmptyState` "Order not found", with no toast and no table.
- [ ] Other failures (5xx, network) show only the sticky toast `Could not load the order`, with no table and no empty state.
- [ ] A **Back to my orders** link to `/orders` (decided) shows in every state.

## Explicit non-goals
- No reorder, cancel or status. The page shows Ordering's prices and total and never asks Catalog.
- No client-side id validation (decided): `/orders/abc` is sent to Ordering, and its 400 is shown as not found.
- No change to `apphost.mts`, `vite.config.ts`, `format.ts`, `errors.ts`, the router, `App.vue`, `AppHeader.vue`, the basket store, `OrdersView.vue`, the other services or views, or the dependencies. `listByBuyer` and `OrderSummary` don't change.
- No orders store, no caching between `/orders` and `/orders/:id`, no polling, and no reload when only the `:id` param changes (no in-app link goes from one detail to another).
- Prices come only from what Basket and Ordering return. No page asks Catalog for a price again, and no request body sends a price to any service (adding sends only `productId` and `quantity`).
- Checkout is one call to Basket (`POST /basket/{buyerId}/checkout`). No page calls Ordering to create anything (no `POST /orders`).
- No page reads a database. The browser talks only to the storefront origin, through `/api/{service}`.
- No changes to catalog, basket, ordering, postgres, redis or their AppHost registrations.
- No AppHost reference to a service this spec's pages don't call.
- Nothing from PROJECT.md §9: no login or auth, payments, shipping, product admin or images, stock, order status, checkout form, re-pricing, retries or fallbacks, order paging, or frontend tests.

## Data model
None on the server, and no new browser storage.

**Request sent by `/orders/:id`:** one `GET /api/ordering/orders/{id}` per visit, where `{id}` is the route param as typed (the proxy strips `/api/ordering`, so Ordering sees `GET /orders/{id}`). The page sends no other Ordering request.

**Response read** (types added to `src/services/OrderingService.ts`):

```ts
/** One order line: the name and unit price are the snapshot Basket took from Catalog when the item was added. */
export interface OrderItem {
  productId: number;
  productName: string;
  unitPrice: number;
  quantity: number;
}

/** `GET /orders/{id}`: one order with its lines, in the order they were added. */
export interface Order {
  id: number;
  buyerId: string;
  createdAt: string; // ISO-8601 UTC
  total: number;
  items: OrderItem[];
}
```

**`OrderingService.get(id: string): Promise<Order>`** → `this.request.get(encodeURIComponent(id))`. Unknown id → axios rejects with `response.status` 404, and non-numeric → 400.

**`/orders/:id` view** (`OrderDetailView.vue`): `id = String(route.params.id)`.

| When | Renders after `<h1>Order #{{ id }}</h1>` |
|---|---|
| `loading` | `<ECard loading />` |
| `notFound` (response status 404 or 400) | `<EEmptyState icon="receipt_long" title="Order not found" />` |
| `failed` (any other error) | nothing (the toast `Could not load the order` is the error) |
| loaded | `<p class="order-date" data-testid="order-date">Placed {{ formatDate(order.createdAt) }}</p>` + the table below |

In every state, a `<RouterLink to="/orders" class="order-back" data-testid="order-back">Back to my orders</RouterLink>` comes last.

**Table:** `<table class="cart-table" aria-label="Order lines">` inside an `ECard`, with the same classes and styles as `/checkout`:

| Column | Header | Cell |
|---|---|---|
| Product | `Product` | `item.productName` |
| Unit price | `Unit price` (right) | `formatPrice(item.unitPrice)` (`data-testid="line-unit-price"`) |
| Quantity | `Quantity` (right) | `item.quantity` (`data-testid="line-quantity"`) |
| Line total | `Line total` (right) | `formatPrice(item.unitPrice * item.quantity)` (`data-testid="line-total"`) |

There is one `<tr data-testid="order-line">` per item, keyed by index, in response order. The `tfoot` row is `<th scope="row" colspan="3">Total</th>` + `<td data-testid="order-total">{{ formatPrice(order.total) }}</td>`: Ordering's `total`, never a client-side sum.

## Tasks
1. **Service** (`src/vue-front/storefront/src/services/OrderingService.ts`, modified): add the `OrderItem` and `Order` interfaces and `get(id)` as in Data model, with doc comments in the existing style. Leave `OrderSummary` and `listByBuyer` as they are.
2. **Detail script** (`src/vue-front/storefront/src/views/OrderDetailView.vue`): `<script setup lang="ts">`. `const id = String(useRoute().params.id)`, `order = ref<Order | null>(null)`, `loading = ref(true)`, `notFound = ref(false)`, `failed = ref(false)`. A local `httpStatus(error: unknown): number | undefined` reads `error.response?.status`. In `onMounted`, create `new OrderingService()` (after `Core.init`), then `try { order.value = await service.get(id) } catch (error) { const s = httpStatus(error); if (s === 404 || s === 400) notFound.value = true; else { failed.value = true; showError('Could not load the order', error) } } finally { loading.value = false }`. Add a top comment: one GET on mount, Ordering's lines, prices and total shown as-is, and 404/400 meaning not found.
3. **Detail template:** `<h1>Order #{{ id }}</h1>`, then the four states from Data model, then the back link.
4. **Detail styles** (scoped): copy `/checkout`'s `.cart-table` rules (including `tfoot`), plus `.order-date { margin: 0 0 0.75rem; color: var(--e-color-text-secondary, #5b6271); }` and `.order-back { display: inline-block; margin-top: 0.75rem; }`.

## Files touched
- `src/vue-front/storefront/src/services/OrderingService.ts` (modified)
- `src/vue-front/storefront/src/views/OrderDetailView.vue` (modified)

Nothing else changes: not `src/apphost.mts`, `vite.config.ts`, `format.ts`, `errors.ts`, the router, the store, `App.vue`, the components, the other views or services, `package.json` / `package-lock.json`, and nothing in catalog, basket or ordering.

## Definition of Done
Run every check from the repository root unless it says otherwise.
- `<web>` is the storefront URL from the dashboard (storefront → Endpoints).
- `B` is the buyerId, read with the Playwright MCP `browser_evaluate` → `localStorage.getItem('eshop.buyerId')`. In Git Bash, set `B=<that value>`.
- Browser checks use the Playwright MCP (`browser_navigate`, `browser_snapshot`, `browser_click`, `browser_console_messages`, `browser_network_requests`, `browser_evaluate`).
- **Match Ordering** means that `curl -s <web>/api/ordering/orders/<id>` returns an object O where:
  - the page has exactly `O.items.length` `order-line` rows, in the same order;
  - row i shows `O.items[i].productName`, its `line-unit-price` is `O.items[i].unitPrice` in en-US dollars, its `line-quantity` is `O.items[i].quantity`, and its `line-total` is `O.items[i].unitPrice × O.items[i].quantity` in en-US dollars;
  - `order-total` is `O.total` in en-US dollars;
  - `order-date` equals `'Placed ' + new Intl.DateTimeFormat('en-US', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(O.createdAt))`, run in `browser_evaluate`.

1. **AppHost build:** in `src/`, `npm run build` exits 0 with no eslint or tsc errors.
2. **Storefront build:** in `src/vue-front/storefront`, `npm ci && npm run build` exits 0, `vue-tsc` reports no errors, and `dist/index.html` exists. No Java service is touched, so there's no `mvn test`.
3. **Healthy start:** in `src/`, `aspire run` shows every resource (postgres, catalogdb, orderingdb, pgAdmin, redis, catalog, basket, ordering, storefront) as Running/Healthy.
4. **Seed an order:** `browser_navigate` to `<web>/`, `browser_evaluate` → `localStorage.clear()`, `browser_navigate` to `<web>/` again and read `B`. In Git Bash:
   - `curl -s -X POST -H 'Content-Type: application/json' -d '{"productId":1,"quantity":2}' <web>/api/basket/basket/$B/items` → 200;
   - `curl -s -X POST -H 'Content-Type: application/json' -d '{"productId":2,"quantity":1}' <web>/api/basket/basket/$B/items` → 200;
   - `curl -s -X POST <web>/api/basket/basket/$B/checkout` → 201 `{"orderId":<id>}`.
5. **Detail from the list:** `browser_navigate` to `<web>/orders`, then click `Order #<id>`. The URL is `<web>/orders/<id>`, the heading is `Order #<id>`, and the snapshot shows a table `Order lines` with two `order-line` rows (product 1 with quantity `2`, then product 2 with quantity `1`). The page Matches Ordering.
6. **Back link:** click **Back to my orders**. The URL is `<web>/orders` and the `Orders` table lists `Order #<id>`.
7. **Direct load:** `browser_navigate` to `<web>/orders/<id>` (a fresh load, not an in-app navigation). The page Matches Ordering again.
8. **Not found:**
   - `curl -s -o /dev/null -w '%{http_code}' <web>/api/ordering/orders/999999999` prints `404`. `browser_navigate` to `<web>/orders/999999999`. The snapshot shows the heading `Order #999999999`, `Order not found` and a **Back to my orders** link. There is no `Order lines` table and no `alert`.
   - `curl -s -o /dev/null -w '%{http_code}' <web>/api/ordering/orders/abc` prints `400`. `browser_navigate` to `<web>/orders/abc`. The snapshot shows the heading `Order #abc` and `Order not found`, with no table and no `alert`.
9. **Network:** clear the log by reading it with `browser_network_requests`, then `browser_navigate` to `<web>/orders/<id>`. Among the requests to `/api/ordering` there is exactly one: `GET <web>/api/ordering/orders/<id>` → 200. There is no `POST`, `PUT` or `DELETE` to any `/api/` path and no `/api/catalog` request. App.vue's existing `GET /api/basket/basket/<B>` is expected.
10. **Load failure:** stop `ordering` in the dashboard, then `browser_navigate` to `<web>/orders/<id>`. Within 15 s, the snapshot shows an `alert` with `Could not load the order` and a detail starting with `HTTP 5`. There is no `Order lines` table and no `Order not found`, and **Back to my orders** shows. Start `ordering` again and wait until it is Healthy. Reloading `<web>/orders/<id>` shows the order again.
11. **Console:** across checks 5–7 and 9, `browser_console_messages` has no errors or warnings from the app. The browser's own failed-resource messages for the 404/400 in check 8 and the 5xx in check 10 are expected.
12. **Scope fence:** `git diff --stat main...HEAD` lists only the two files in Files touched, plus files under `_agenticdev/`. `git diff main...HEAD -- src/apphost.mts src/java-back src/vue-front/storefront/vite.config.ts src/vue-front/storefront/package.json src/vue-front/storefront/package-lock.json src/vue-front/storefront/src/App.vue src/vue-front/storefront/src/components src/vue-front/storefront/src/router src/vue-front/storefront/src/stores src/vue-front/storefront/src/format.ts src/vue-front/storefront/src/errors.ts src/vue-front/storefront/src/services/BasketService.ts src/vue-front/storefront/src/services/CatalogService.ts src/vue-front/storefront/src/services/CategoryService.ts src/vue-front/storefront/src/views/HomeView.vue src/vue-front/storefront/src/views/CartView.vue src/vue-front/storefront/src/views/CheckoutView.vue src/vue-front/storefront/src/views/OrdersView.vue` is empty.

## References
- PROJECT.md:
  - §6 Storefront: the `/orders/:id` page shows the order detail (lines, total and date) and calls ordering `GET /orders/{id}`;
  - §4.3 Ordering: `GET /orders/{id}` → `Order = { id, buyerId, createdAt, total, items[] }`, or 404;
  - §2 Architecture (the browser talks only to the storefront origin through `/api/{service}`, and errors propagate to the UI);
  - §5 step 6 (after checkout the storefront navigates to `/orders/{orderId}`, built in storefront/13);
  - §9 (no order status, no frontend tests);
  - §10 journey step 4 (the order detail page shows the snapshot prices and the correct total).
- PLAN.md Phase 5 → Storefront, spec 12 (`12-order-detail.md`), the other half of the old 5.5. "References": ordering's reference and proxy entry were already added in 11.
- Patterns: `microservices/storefront/11-my-orders.md` (the Ordering service, local load state and the DoD style) and `microservices/storefront/10-checkout-review.md` (the lines table with a `tfoot` total).
