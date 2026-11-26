# storefront 11 — my orders

**Status:** Not started

## Goal
List this buyer's orders on `/orders`. This is the first page that calls Ordering, so it adds the Ordering reference and proxy.

## Context
- storefront/10 is merged (f6d47ad). `src/vue-front/storefront/src/views/OrdersView.vue` is still the storefront/01 placeholder: only `<h1>My orders</h1>`. The router already maps `/orders` → `OrdersView` (name `orders`) and `/orders/:id` → `OrderDetailView` (name `order-detail`, a placeholder `<h1>Order #{{ id }}</h1>` until storefront/12). The header already has a **My orders** link to `/orders` (`AppHeader.vue`).
- `src/apphost.mts`: the `storefront` resource (`addViteApp`) has `withEnvironment('services__basket__http__0', basket.getEndpoint('http'))` + `waitFor(basket)` and the same pair for catalog. Its comment says "Ordering is added by the spec whose pages call it (storefront 11)". The `ordering` const already exists (`addDockerfile('ordering', …)`).
- `src/vue-front/storefront/vite.config.ts`: `serviceUrls` maps `basket` and `catalog` to `process.env.services__<name>__http__0`. `proxyEntries()` builds one `/api/<name>` entry per set URL, with the prefix stripped and `proxyTimeout: 10_000` (a stopped service gives a 502 instead of hanging).
- Services follow one pattern (`BasketService.ts`, `CatalogService.ts`): `extends ApiService` from `@guilherme/vue-base`, `config()` returns `{ baseURL: \`${Core.baseURL}/<service>\`, resource }`, and they are constructed only after `Core.init`.
- `src/buyer.ts` `getBuyerId()` is the only reader of `localStorage['eshop.buyerId']`. `src/errors.ts` `showError(message, error)` shows a sticky vue-base error toast with the detail `HTTP <status> <text>`. `src/format.ts` has `formatPrice` (en-US dollars). vue-base's own date formatters are pt-BR, so they aren't used.
- `HomeView.vue` loads with local `loading` / `failed` refs and `showError` on failure. `/cart` and `/checkout` show `<ECard loading />`, `EEmptyState` + a `RouterLink`, and a `.cart-table` inside an `ECard`.
- Ordering (ordering/03, Done fb347c9): `GET /orders?buyerId={uuid}` → 200 `[{ id, createdAt, total, itemCount }]`, ordered by `createdAt` desc then `id` desc and not paged. `createdAt` is an ISO-8601 UTC string, `itemCount` is the sum of the line quantities, and an unknown buyer gets `[]`. A missing or non-UUID `buyerId` is 400.

## Depends on
- `storefront/01-storefront-shell`
- `ordering/03-list-buyer-orders`

## Scope
- [ ] AppHost: storefront gets `withEnvironment('services__ordering__http__0', ordering.getEndpoint('http'))` + `waitFor(ordering)` (not `withReference`, which Dockerfile containers reject, PROJECT §7).
- [ ] `vite.config`: proxy `/api/ordering` → `services__ordering__http__0`, with the prefix stripped.
- [ ] `src/services/OrderingService.ts` (decided): `ApiService` with base `/api/ordering`, resource `orders`, and `listByBuyer(buyerId)` → `GET /orders?buyerId=…`.
- [ ] `/orders` GETs once on mount with local `loading` / `failed` refs, as `HomeView` does (decided: no store). It renders Ordering's order as-is (newest first) and never sorts on the client.
- [ ] A table inside an `ECard`, styled like `/cart` (decided), with one row per order: **Order** (`RouterLink` "Order #<id>" to `/orders/<id>`, decided), **Date** (`formatDate(createdAt)`), **Items** (`itemCount` as a plain number, decided) and **Total** (`formatPrice(total)`).
- [ ] `formatDate` in `src/format.ts` (decided): en-US with `dateStyle: 'medium'` and `timeStyle: 'short'`, in the browser's timezone.
- [ ] States (decided): `<ECard loading />` while loading. When the list is empty, `EEmptyState` "No orders yet" plus a **Browse products** link to `/`. On failure, only the `Could not load your orders` toast, with no table and no empty state.

## Explicit non-goals
- No order detail content (storefront/12). `OrderDetailView.vue` stays a placeholder. The page only sends `GET` requests to Ordering.
- No orders store, no client-side sorting or filtering, no polling and no paging (decided).
- No change to the router, `App.vue`, `AppHeader.vue`, the basket store, the existing services or views, or the dependencies.
- Prices come only from what Basket and Ordering return. No page asks Catalog for a price again, and no request body sends a price to any service (adding sends only `productId` and `quantity`).
- Checkout is one call to Basket (`POST /basket/{buyerId}/checkout`). No page calls Ordering to create anything (no `POST /orders`).
- No page reads a database. The browser talks only to the storefront origin, through `/api/{service}`.
- No changes to catalog, basket, ordering, postgres, redis or their AppHost registrations.
- No AppHost reference to a service this spec's pages don't call.
- Nothing from PROJECT.md §9: no login or auth, payments, shipping, product admin or images, stock, order status, checkout form, re-pricing, retries or fallbacks, order paging, or frontend tests.

## Data model
None on the server, and no new browser storage. `eshop.buyerId` is still read only through `getBuyerId()`.

**Request sent by `/orders`:** one `GET /api/ordering/orders?buyerId={buyerId}` per visit (the proxy strips `/api/ordering`, so Ordering sees `GET /orders?buyerId=…`). It sends no other Ordering request.

**Response read** (types in `src/services/OrderingService.ts`):

```ts
/** `GET /orders?buyerId=`: one order without its lines. itemCount is the sum of the line quantities. */
export interface OrderSummary {
  id: number;
  createdAt: string; // ISO-8601 UTC, e.g. "2026-11-26T15:04:05.123456Z"
  total: number;
  itemCount: number;
}
```

**`OrderingService`:** `config()` → `{ baseURL: \`${Core.baseURL as string}/ordering\`, resource: 'orders' }`. `listByBuyer(buyerId: string): Promise<OrderSummary[]>` → `this.request.get('', { params: { buyerId } })`.

**`formatDate`** (`src/format.ts`): `new Intl.DateTimeFormat('en-US', { dateStyle: 'medium', timeStyle: 'short' })`. `formatDate('2026-11-26T15:04:00Z')` gives `Nov 26, 2026, 3:04 PM` in UTC (the browser's local time otherwise).

**`/orders` table** (`OrdersView.vue`): `<table class="orders-table" aria-label="Orders">` inside an `ECard`:

| Column | Header | Cell |
|---|---|---|
| Order | `Order` | `<RouterLink :to="\`/orders/${order.id}\`" data-testid="order-link">Order #{{ order.id }}</RouterLink>` |
| Date | `Date` | `formatDate(order.createdAt)` (`data-testid="order-date"`) |
| Items | `Items` (right-aligned) | `order.itemCount` (`data-testid="order-items"`) |
| Total | `Total` (right-aligned) | `formatPrice(order.total)` (`data-testid="order-total"`) |

There is one `<tr data-testid="order-row">` per order, keyed by `id`, in response order. There is no `<tfoot>`.

**States:**

| When | Renders after `<h1>My orders</h1>` |
|---|---|
| `loading` | `<ECard loading />` |
| `failed` | nothing (the toast `Could not load your orders` is the error) |
| loaded, `orders.length === 0` | `<EEmptyState icon="receipt_long" title="No orders yet" />` + `<RouterLink to="/" class="orders-browse">Browse products</RouterLink>` |
| loaded, has orders | the table above |

## Tasks
1. **AppHost** (`src/apphost.mts`): on the `storefront` builder, after the catalog pair, add `.withEnvironment('services__ordering__http__0', ordering.getEndpoint('http'))` and `.waitFor(ordering)`. Update the comment above it: the browser reaches Basket, Catalog and Ordering through `/api/basket`, `/api/catalog` and `/api/ordering`, and drop the "Ordering is added by …" sentence.
2. **Proxy** (`src/vue-front/storefront/vite.config.ts`): add `ordering: process.env.services__ordering__http__0` to `serviceUrls`. `proxyEntries()` doesn't change.
3. **Formatter** (`src/vue-front/storefront/src/format.ts`): add `formatDate(iso: string): string` as in Data model, with a doc comment.
4. **Service** (`src/vue-front/storefront/src/services/OrderingService.ts`, new): the `OrderSummary` interface and `OrderingService` as in Data model, with doc comments in the style of `BasketService.ts`.
5. **Orders script** (`src/vue-front/storefront/src/views/OrdersView.vue`): `<script setup lang="ts">`. `orders = ref<OrderSummary[]>([])`, `loading = ref(true)`, `failed = ref(false)`. In `onMounted`, create `new OrderingService()` (after `Core.init`), then `try { orders.value = await service.listByBuyer(getBuyerId()) } catch (error) { failed.value = true; showError('Could not load your orders', error) } finally { loading.value = false }`. Add a top comment: one GET on mount, rows in Ordering's order (newest first), and a link to each detail (storefront/12).
6. **Orders template:** `<h1>My orders</h1>`, then the four states from Data model.
7. **Orders styles** (scoped): `.orders-table` with the same rules as `/cart`'s `.cart-table` (full width, collapsed borders, cell padding, row bottom border, `.orders-table__num` right-aligned, no `tfoot` rule), plus `.orders-browse { display: inline-block; margin-top: 0.75rem; }`.

## Files touched
- `src/apphost.mts` (modified: storefront registration only)
- `src/vue-front/storefront/vite.config.ts` (modified)
- `src/vue-front/storefront/src/format.ts` (modified)
- `src/vue-front/storefront/src/services/OrderingService.ts` (created)
- `src/vue-front/storefront/src/views/OrdersView.vue` (modified)

No router, store, other service, other view, `App.vue`, `AppHeader.vue` or dependency changes (`package.json` and `package-lock.json` are untouched). Nothing in catalog, basket or ordering changes.

## Definition of Done
Run every check from the repository root unless it says otherwise.
- `<web>` is the storefront URL from the dashboard (storefront → Endpoints).
- `B` is the buyerId, read with the Playwright MCP `browser_evaluate` → `localStorage.getItem('eshop.buyerId')`. In Git Bash, set `B=<that value>`.
- Browser checks use the Playwright MCP (`browser_navigate`, `browser_snapshot`, `browser_click`, `browser_console_messages`, `browser_network_requests`, `browser_evaluate`).
- **Match Ordering** means that `curl -s "<web>/api/ordering/orders?buyerId=$B"` returns an array A where:
  - the page has exactly `A.length` `order-row` rows, in the same order;
  - row i's `order-link` text is `Order #<A[i].id>` and its `href` ends with `/orders/<A[i].id>`;
  - row i's `order-items` is `A[i].itemCount`, and its `order-total` is `A[i].total` in en-US dollars;
  - row i's `order-date` equals `new Intl.DateTimeFormat('en-US', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(A[i].createdAt))`, run in `browser_evaluate`.

1. **AppHost build:** in `src/`, `npm run build` exits 0 with no eslint or tsc errors.
2. **Storefront build:** in `src/vue-front/storefront`, `npm ci && npm run build` exits 0, `vue-tsc` reports no errors, and `dist/index.html` exists. No Java service is touched, so there's no `mvn test`.
3. **Healthy start:** in `src/`, `aspire run` shows every resource (postgres, catalogdb, orderingdb, pgAdmin, redis, catalog, basket, ordering, storefront) as Running/Healthy.
4. **Env:** in the dashboard, storefront → Environment variables lists `services__ordering__http__0` with ordering's HTTP endpoint, next to `services__basket__http__0` and `services__catalog__http__0`.
5. **Proxy:** `curl -s -o /dev/null -w '%{http_code}' "<web>/api/ordering/orders?buyerId=00000000-0000-4000-8000-000000000000"` prints `200`, and the body is `[]`.
6. **Empty:** `browser_navigate` to `<web>/`, then `browser_evaluate` → `localStorage.clear()`, then `browser_navigate` to `<web>/orders`. The snapshot shows the heading `My orders`, `No orders yet` and a **Browse products** link, and there is no `Orders` table. Clicking **Browse products** opens `<web>/`.
7. **Seed two orders:** read `B` (it is the new buyerId after check 6). In Git Bash:
   - `curl -s -X POST -H 'Content-Type: application/json' -d '{"productId":1,"quantity":1}' <web>/api/basket/basket/$B/items` → 200, then `curl -s -X POST <web>/api/basket/basket/$B/checkout` → 201 `{"orderId":<first>}`;
   - `curl -s -X POST -H 'Content-Type: application/json' -d '{"productId":2,"quantity":3}' <web>/api/basket/basket/$B/items` → 200, then `curl -s -X POST <web>/api/basket/basket/$B/checkout` → 201 `{"orderId":<second>}`.
8. **List:** `browser_navigate` to `<web>/orders`. The snapshot shows a table `Orders` with two `order-row` rows. Row 0 is `Order #<second>` with Items `3`, and row 1 is `Order #<first>` with Items `1`. The rows Match Ordering.
9. **Link:** click `Order #<first>`. The URL is `<web>/orders/<first>`, and the storefront/12 placeholder heading `Order #<first>` shows. Go back with `browser_navigate_back`, and the list shows again.
10. **Network:** clear the log by reading it with `browser_network_requests`, then `browser_navigate` to `<web>/orders`. Among the requests to `/api/ordering`, there is exactly one: `GET <web>/api/ordering/orders?buyerId=<B>` → 200. There is no `POST`, `PUT` or `DELETE` to any `/api/` path, and no `/api/catalog` request. App.vue's existing `GET /api/basket/basket/<B>` is expected.
11. **Load failure:** stop `ordering` in the dashboard, then `browser_navigate` to `<web>/orders`. Within 15 s, the snapshot shows an `alert` with `Could not load your orders` and a detail starting with `HTTP 5`, and there is no `Orders` table and no `No orders yet`. Start `ordering` again and wait until it is Healthy. Reloading `/orders` shows the two rows.
12. **Console:** across checks 6–10, `browser_console_messages` has no errors or warnings from the app (the failed request in 11 is expected).
13. **Scope fence:** `git diff --stat main...HEAD` lists only the five files in Files touched, plus files under `_agenticdev/`. `git diff main...HEAD -- src/apphost.mts` changes only the storefront block and its comment. `git diff main...HEAD -- src/java-back src/vue-front/storefront/package.json src/vue-front/storefront/package-lock.json src/vue-front/storefront/src/App.vue src/vue-front/storefront/src/components src/vue-front/storefront/src/router src/vue-front/storefront/src/stores src/vue-front/storefront/src/services/BasketService.ts src/vue-front/storefront/src/services/CatalogService.ts src/vue-front/storefront/src/services/CategoryService.ts src/vue-front/storefront/src/views/HomeView.vue src/vue-front/storefront/src/views/CartView.vue src/vue-front/storefront/src/views/CheckoutView.vue src/vue-front/storefront/src/views/OrderDetailView.vue` is empty.

## References
- PROJECT.md:
  - §6 Storefront: the `/orders` page lists this buyer's orders (date, item count, total), each linking to its detail, and calls ordering `GET /orders?buyerId=`;
  - §4.3 Ordering: `GET /orders?buyerId={uuid}` → `OrderSummary[] = { id, createdAt, total, itemCount }`, newest first and not paged;
  - §2 Architecture (the browser talks only to the storefront origin through `/api/{service}`, and errors propagate to the UI);
  - §3 Identity (`buyerId`);
  - §7 AppHost (storefront → ordering, and `withEnvironment('services__<name>__http__0', …)` + `waitFor` instead of `withReference` for Dockerfile containers);
  - §9 (no order history paging and no frontend tests);
  - §10 journey step 5 (`/orders` lists the order).
- PLAN.md Phase 5 → Storefront, spec 11 (`11-my-orders.md`), half of the old 5.5. "References": ordering's reference and proxy entry are added in 11.
- Patterns: `microservices/storefront/02-cart-badge.md` (the first service reference and its proxy entry), `microservices/storefront/03-product-grid.md` (a service class and local load state) and `microservices/storefront/10-checkout-review.md` (the table, its states and the DoD style).
