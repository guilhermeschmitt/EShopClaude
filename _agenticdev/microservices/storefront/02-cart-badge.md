# storefront 02 — cart badge

**Status:** Done (049f4be)

## Goal
Show the buyer's basket in the header as a badge with the sum of item quantities. The basket is loaded from Basket and kept in shared state that later pages refresh. This spec also adds the shared error display, because this is the first call that can fail. It is the first spec whose pages call a service, so it also adds the Basket reference and the Basket proxy.

## Context
- storefront/01 is merged (cbfcc65). `src/vue-front/storefront/` has the shell: `main.ts` (`getBuyerId()`, then `await Core.init(app, router, routes, { baseURL: '/api' })`, then `app.use(router)` and mount), `App.vue` (`<AppHeader />` + `<RouterView />`), `components/AppHeader.vue` (a cart `RouterLink` to `/cart` with `aria-label="Cart"` and `<EIcon icon="shopping_cart" />`, no badge), `buyer.ts` (`getBuyerId()`), and five placeholder views. No page calls a service yet.
- `vite.config.ts` has only `@vitejs/plugin-vue` and the `@` alias. Its comment says the `/api/<service>` forwarding is added by the first page that calls that service, and that page is this spec's.
- `src/apphost.mts`: `basket` is registered with `await builder.addDockerfile('basket', './java-back/basket')…`, a chain that isn't assigned to a variable. `storefront` is registered with `addViteApp('storefront', './vue-front/storefront')`, `.withNpm({ install: true })`, `.withExternalHttpEndpoints()` and `.withHttpHealthCheck({ path: '/' })`, with no references or waits. Basket already shows the pattern for a Java peer: `.withEnvironment('services__<name>__http__0', <name>.getEndpoint('http'))` + `.waitFor(<name>)`. `withReference` is rejected for Dockerfile containers (PROJECT §7).
- basket/01 is merged. `GET /basket/{buyerId}` returns `{ buyerId, items: [{ productId, productName, unitPrice, quantity }], total }`, and returns an empty basket (`items: []`, `total: 0`) when the key is missing. basket/02 (`POST /basket/{buyerId}/items`) is also merged. The Definition of Done uses it to put items in the basket, but no page here calls it.
- What vue-base `v0.0.1` ships, read from `node_modules/@guilherme/vue-base/dist/my-vue-core.js`:
  - `ApiService` is a named export. A subclass implements `config()` returning `{ baseURL, resource }`. The constructor calls `config()` right away and builds an axios instance with base URL `` `${baseURL}/${resource}` `` and `Content-Type: application/json`, exposed as `this.request`. axios itself isn't exported. The request interceptor adds a `Bearer` header only when vue-base's auth holds an access token, and it never does here because nobody logs in.
  - `Core.baseURL` returns the `baseURL` passed to `Core.init` (`'/api'`). It is `undefined` until `Core.init` runs, so an `ApiService` must not be constructed at module import time.
  - `useToast()` returns `{ toasts, add, remove, success, error, info, warn }`. `add({ severity, summary, detail, sticky, life })` queues a toast, and non-sticky toasts disappear after 4 s. The default summaries of `error()` and the others are Portuguese, so this app always calls `add` with its own English summary.
  - `EToast` is registered globally by `Core.init`. It teleports to `body` and renders each toast as `role="alert"`, with the summary and detail as `<p>` elements and a close button labeled `Fechar`. It must be mounted once in the app.
  - `unreadBadge(n)` is a named export: `''` for 0, `'99+'` above 99, `String(n)` otherwise.
- When a resource is stopped in the dashboard, Aspire's endpoint proxy keeps accepting TCP connections on its port but never answers. Found while verifying this spec: `curl` to both `<web>/api/basket/…` and basket's own port hung with no response. A proxy entry without a timeout therefore never fails, so each `/api/<service>` entry sets `proxyTimeout`. Vite answers a timed-out proxy request with `502 Bad Gateway`. This is a timeout, not a retry or a fallback (PROJECT §2, §9).
- `env.d.ts` declares `module '@guilherme/vue-base'` untyped, so its imports are `any`. This spec adds TypeScript types for the basket response only.

## Depends on
- `storefront/01-storefront-shell`
- `basket/01-read-basket`

## Scope
- [ ] AppHost: keep `basket` in a variable (`const basket = await builder.addDockerfile('basket', …)`), with its chain unchanged. This is the only edit to basket's registration.
- [ ] AppHost: storefront gets `withEnvironment('services__basket__http__0', basket.getEndpoint('http'))` + `waitFor(basket)` (not `withReference`, which Dockerfile containers reject, PROJECT §7).
- [ ] `vite.config`: proxy `/api/basket` → `services__basket__http__0`, with the prefix stripped.
- [ ] Shared basket state: `GET /basket/{buyerId}` on start, with a refresh function for later specs.
- [ ] Header badge = sum of item quantities, shown only when it is greater than 0.
- [ ] Shared error display for failed calls: a sticky vue-base `EToast`.

## Explicit non-goals
- No catalog or ordering references or proxies. No cart page content (storefront/08).
- No page calls `POST /items`, `PUT /items/{productId}` or `POST /checkout`. Those belong to storefront/07, 09 and 13.
- Prices come only from what Basket and Ordering return. No page asks Catalog for a price again, and no request body sends a price to any service (adding sends only `productId` and `quantity`).
- Checkout is one call to Basket (`POST /basket/{buyerId}/checkout`). No page calls Ordering to create anything (no `POST /orders`).
- No page reads a database. The browser talks only to the storefront origin, through `/api/{service}`.
- No changes to catalog, basket, ordering, postgres, redis or their AppHost registrations, apart from assigning `basket` to a `const`.
- No AppHost reference to a service this spec's pages don't call.
- No Pinia or other state library, no polling and no refresh on route change. The basket loads once on start, and later specs call `refreshBasket()` after they change it.
- No `withOtlpExporter()` or other OpenTelemetry setup on the storefront (PROJECT.md §2).
- Nothing from PROJECT.md §9: no login or auth, payments, shipping, product admin or images, stock, order status, checkout form, re-pricing, retries or fallbacks, order paging, or frontend tests.

## Data model
None on the server, and no new browser storage. `eshop.buyerId` is still read only through `getBuyerId()`.

**Response shape** read from `GET /api/basket/basket/{buyerId}`, typed in `src/services/BasketService.ts`:

```ts
export interface BasketItem { productId: number; productName: string; unitPrice: number; quantity: number }
export interface Basket { buyerId: string; items: BasketItem[]; total: number }
```

**Shared state** in `src/stores/basket.ts`, which is a module with no library:

| Export | Type | Meaning |
|---|---|---|
| `basket` | `Readonly<Ref<Basket \| null>>` | Last basket loaded. `null` before the first load completes and after a failed load. |
| `itemCount` | `ComputedRef<number>` | Σ `quantity` over `basket.items`, or `0` when `basket` is `null` |
| `refreshBasket()` | `() => Promise<void>` | `GET` the basket for `getBuyerId()` and store it. On failure it sets `basket` to `null`, calls `showError('Could not load the cart', error)` and doesn't rethrow. |
| `useBasket()` | `() => ({ basket, itemCount, refreshBasket })` | Convenience for components |

**Error helper** in `src/errors.ts`: `showError(message: string, error: unknown): void` calls `useToast().add({ severity: 'error', summary: message, detail, sticky: true })`. `detail` is `` `HTTP ${status} ${statusText}` `` when the error has an axios `response`, otherwise the error's `message`, otherwise `'Unknown error'`.

## Tasks
1. **AppHost** (`src/apphost.mts`):
   - Change `await builder.addDockerfile('basket', …)` to `const basket = await builder.addDockerfile('basket', …)`. The chain is unchanged.
   - On the storefront chain, after `.withHttpHealthCheck({ path: '/' })`, add `.withEnvironment('services__basket__http__0', basket.getEndpoint('http'))` and `.waitFor(basket)`. Replace the storefront comment so it says the storefront reaches Basket through the Vite proxy, and that catalog and ordering are added by the specs whose pages call them (03 and 11).
   - Every builder call is awaited (`no-floating-promises`).
2. **Proxy** (`src/vue-front/storefront/vite.config.ts`): read `process.env.services__basket__http__0`. When it's set, add `server.proxy['/api/basket'] = { target, changeOrigin: true, proxyTimeout: 10_000, rewrite: (p) => p.replace(/^\/api\/basket/, '') }`. Without `proxyTimeout`, a stopped basket leaves the request hanging (see Context). When it's unset, as in `npm run build` or a bare `npm run dev`, add no entry. Still no `server.port`. Update the file's comment to match.
3. **Basket client** (`src/services/BasketService.ts`): the `Basket` and `BasketItem` interfaces, and `class BasketService extends ApiService` with `config()` returning `{ baseURL: `${Core.baseURL}/basket`, resource: 'basket' }` and `async get(buyerId: string): Promise<Basket>`, which returns `(await this.request.get(buyerId)).data`. The class is constructed lazily, on the first call from the store, never at import time.
4. **Error helper** (`src/errors.ts`): `showError` as described in Data model.
5. **Store** (`src/stores/basket.ts`): `basket`, `itemCount`, `refreshBasket` and `useBasket` as described in Data model. `refreshBasket` uses `getBuyerId()` from `@/buyer`.
6. **Shell** (`src/App.vue`): add `<EToast />` once, after `<main>`, and call `void refreshBasket()` in `onMounted`. `refreshBasket` never rejects, so the page renders whether or not the call succeeds.
7. **Badge** (`src/components/AppHeader.vue`): read `itemCount` from `useBasket()`. Inside the cart link, after the icon, render `<span class="app-header__badge" data-testid="cart-badge">{{ unreadBadge(itemCount) }}</span>` only when `itemCount > 0`. The link's `aria-label` is `Cart` when the count is 0 and `` `Cart, ${itemCount} items` `` otherwise. Style it as a small pill positioned over the icon's top-right corner, using vue-base color tokens with hex fallbacks, as the header already does.

## Files touched
- `src/apphost.mts` (modified: `basket` assigned to a `const`, storefront env + wait)
- `src/vue-front/storefront/vite.config.ts` (modified)
- `src/vue-front/storefront/src/App.vue` (modified)
- `src/vue-front/storefront/src/components/AppHeader.vue` (modified)
- `src/vue-front/storefront/src/services/BasketService.ts` (created)
- `src/vue-front/storefront/src/stores/basket.ts` (created)
- `src/vue-front/storefront/src/errors.ts` (created)

No dependency changes (`package.json` and `package-lock.json` are untouched). Nothing in catalog, basket or ordering changes.

## Definition of Done
Run every check from the repository root unless it says otherwise. `<web>` is the storefront URL from the dashboard (storefront → Endpoints). `B` is the buyerId, read in the browser with the Playwright MCP `browser_evaluate` → `localStorage.getItem('eshop.buyerId')`. In Git Bash, set `B=<that value>`. Browser checks use the Playwright MCP (`browser_navigate`, `browser_snapshot`, `browser_console_messages`, `browser_network_requests`, `browser_evaluate`).

1. **AppHost build:** in `src/`, `npm run build` exits 0 with no eslint or tsc errors.
2. **Storefront build:** in `src/vue-front/storefront`, `npm ci && npm run build` exits 0, `vue-tsc` reports no errors, and `dist/index.html` exists. Since no Java service is touched, there's no `mvn test`.
3. **Healthy start:** in `src/`, `aspire run` shows every resource (postgres, catalogdb, orderingdb, pgAdmin, redis, catalog, basket, ordering, storefront) as Running/Healthy. storefront starts only after basket is healthy.
4. **Env:** dashboard → storefront → Environment has `services__basket__http__0` set to basket's `http://localhost:<port>` endpoint, and no `services__catalog__*` or `services__ordering__*`.
5. **Proxy:** `curl -s <web>/api/basket/basket/$B` returns 200 with `{"buyerId":"<B>","items":[],"total":0}` on a fresh buyer.
6. **Empty badge:** `browser_navigate` to `<web>/`. The snapshot shows the cart link named `Cart` and no badge, and `browser_evaluate` → `document.querySelector('[data-testid="cart-badge"]')` is `null`. `browser_console_messages` has no errors or warnings from the app.
7. **Badge count:** `curl -s -X POST -H 'Content-Type: application/json' -d '{"productId":1,"quantity":2}' <web>/api/basket/basket/$B/items`, then the same with `-d '{"productId":2}'`; both return 200. Reload `<web>/`. The snapshot shows the cart link named `Cart, 3 items`, and `browser_evaluate` → `document.querySelector('[data-testid="cart-badge"]').textContent.trim()` is `"3"`.
8. **Loads once:** after the reload in check 7, `browser_network_requests` lists exactly one `/api/` request, `GET <web>/api/basket/basket/<B>` → 200. Every request is same-origin, and no request goes to basket's port directly or to `/api/catalog` or `/api/ordering`. Clicking **My orders** and the cart icon adds no `/api/` request.
9. **Error:** stop `basket` in the dashboard, then reload `<web>/`. `curl -s -m 30 -o /dev/null -w '%{http_code} %{time_total}' <web>/api/basket/basket/$B` prints `502` after about 10 s, not a hang. Within 15 s of the reload, the snapshot shows an `alert` with `Could not load the cart` and the detail `HTTP 502 Bad Gateway`. The browser console's only error is that failed request. The header and the `Products` heading still render, and there's no badge. The alert is still there after 5 s, because it's sticky, and it closes with its close button. Start `basket` again, reload, and the badge shows `3` again with no alert.
10. **Proxy is conditional:** `grep -n "services__basket__http__0" src/vue-front/storefront/vite.config.ts` matches, `grep -n "proxyTimeout" src/vue-front/storefront/vite.config.ts` matches, and `grep -nE "services__(catalog|ordering)" src/vue-front/storefront/vite.config.ts` prints nothing.
11. **Scope fence:** `git diff --stat main...HEAD` lists only `src/apphost.mts`, files under `src/vue-front/storefront/src/`, `src/vue-front/storefront/vite.config.ts` and files under `_agenticdev/`. `git diff main...HEAD -- src/apphost.mts` shows only `const basket =` on the basket line, the two added storefront calls and the storefront comment.

## References
- PROJECT.md §2 Architecture (the browser talks only to the storefront origin, `/api/{service}` proxied to `services__{name}__http__0` with the prefix stripped, errors propagate to the UI, no retries), §3 Identity without login (`buyerId`), §4.2 Basket (`GET /basket/{buyerId}`, response shape), §6 Storefront (header cart icon with an item-count badge that is the sum of quantities, vue-base components and bundled axios), §7 Aspire AppHost (storefront → basket, `withEnvironment` + `waitFor` in place of `withReference` for Dockerfile containers), §9 out of scope.
- PLAN.md Phase 5 → Storefront, spec 2 (`02-cart-badge.md`). It replaces the old 5.1. Its "References" bullet says basket is referenced in 02.
