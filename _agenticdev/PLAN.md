# EShopClaude — Build Plan

Source of truth: [PROJECT.md](PROJECT.md). This plan orders the work; PROJECT.md defines the behavior.
Section refs like (§4.1) point to PROJECT.md.

## How to use this plan
- **One step = one feature branch** off `main`, named `feature/<step-id>-<slug>` (e.g. `feature/3.1-catalog-api`). Merge to `main` only when the step's **Verify** list passes.
- **Projects** in this plan:

  | Name | Path | Aspire resource |
  |---|---|---|
  | AppHost | `src/apphost.mts`, `src/aspire.config.json` | — |
  | Catalog.Api | `src/java-back/catalog/` (Maven) | `catalog` |
  | Ordering.Api | `src/java-back/ordering/` (Maven) | `ordering` |
  | Basket.Api | `src/java-back/basket/` (Maven) | `basket` |
  | Storefront | `src/vue-front/storefront/` (Vue 3 + Vite + vue-base) | `storefront` |

- **Standard checks** referenced below:
  - `AppHost build`: `npm run build` in `src/` (eslint + tsc on `apphost.mts`).
  - `mvn test`: `mvn -f src/java-back/<service>/pom.xml test` (Docker running for Testcontainers).
  - `aspire run`: from `src/`; every resource Running/Healthy in the dashboard.
  - `HTTP via proxy`: `curl http://localhost:<storefront-port>/api/<service>/...` (port from the dashboard).
  - `Trace`: Aspire dashboard → Traces / Structured logs.
  - `Browser`: storefront opened in the browser (Playwright MCP for repeatable checks: snapshot + network requests).

---

## Phase 1 — Project skeleton

### 1.1 Remove the starter
- Delete `src/api/` and `src/frontend/`.
- In `apphost.mts`, remove the `app` (`addNodeApp`) and `frontend` (`addViteApp`) resources and the `publishWithContainerFiles(...)` call; keep `createBuilder` / `build().run()`.
- **Projects:** AppHost.
- **Verify:** AppHost build passes; `aspire run` opens an empty dashboard with no errors.

### 1.2 Java service skeletons (catalog, ordering, basket)
- For each service: standalone Maven project, parent `spring-boot-starter-parent`, Java 21, `spring-boot-starter-web`, `spring-boot-starter-test`; package `br.com.guilherme.eshop.{service}` with empty `api`/`domain`/`repository` (+ `client` for basket) packages; `application.yml` with `server.port: 8080`. No Spring Security (§8).
- Multi-stage `Dockerfile` per service: Maven + JDK 21 build stage → JRE 21 runtime that downloads `opentelemetry-javaagent.jar` and starts with `-javaagent` (§8).
- `.gitignore` for `target/`.
- **Projects:** Catalog.Api, Ordering.Api, Basket.Api.
- **Verify:** `mvn test` (context-loads test) in each; `docker build src/java-back/<service>` succeeds for each.

### 1.3 Storefront skeleton
- Create `src/vue-front/storefront/` with Vue 3 + Vite + vue-base; `Core.init(app, router, routes, { baseURL: '/api' })` with all routes public (no auth guards) (§6).
- `vite.config`: proxy `/api/catalog`, `/api/basket`, `/api/ordering` to `services__{name}__http__0`, stripping the `/api/{service}` prefix (§2).
- `buyerId` bootstrap: generate UUID v4 on first load, persist under `localStorage['eshop.buyerId']` (§3).
- Placeholder route components for `/`, `/cart`, `/checkout`, `/orders`, `/orders/:id`.
- **Projects:** Storefront.
- **Verify:** `npm run build` in `src/vue-front/storefront`; `npm run dev` serves the placeholder home page; `eshop.buyerId` appears in localStorage and survives reload.

### 1.4 Wire services and storefront in the AppHost
- `catalog`, `ordering`, `basket` via `addDockerfile(...)` with an HTTP endpoint on container port 8080.
- `storefront` via `addViteApp("./vue-front/storefront")` with external HTTP endpoints; references + waits to catalog, basket, ordering.
- `basket` references + waits to catalog and ordering (gives `services__catalog__http__0`, `services__ordering__http__0`); map them in basket `application.yml` to `catalog.url` / `ordering.url`.
- **Projects:** AppHost, Basket.Api (`application.yml` only).
- **Verify:** AppHost build; `aspire run` shows catalog, ordering, basket, storefront Running; HTTP via proxy `GET /api/catalog/` returns Spring's 404 JSON (proves proxy + service discovery); dashboard shows OTel logs/metrics from each Java service (proves the agent + `OTEL_*` env).

---

## Phase 2 — Data persistence

### 2.1 PostgreSQL, databases and pgAdmin
- Add `Aspire.Hosting.PostgreSQL` to `aspire.config.json`.
- `postgres` resource with `catalogdb` and `orderingdb`, `withPgAdmin()`.
- catalog → catalogdb, ordering → orderingdb (reference + waitFor).
- Build the JDBC env contract from endpoint + parameters: `SPRING_DATASOURCE_URL` (`jdbc:postgresql://host:port/<db>`), `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` (§7). Check the generated TS API in `src/.aspire/modules/` for the endpoint/expression helpers before writing it.
- **Projects:** AppHost.
- **Verify:** AppHost build; `aspire run` shows postgres and pgAdmin healthy; pgAdmin lists `catalogdb` and `orderingdb`; dashboard resource details show the three `SPRING_DATASOURCE_*` vars on catalog and ordering with a valid JDBC URL.

### 2.2 Redis
- Add `Aspire.Hosting.Redis` to `aspire.config.json`; `redis` resource.
- basket → redis (reference + waitFor) with `SPRING_DATA_REDIS_HOST` / `SPRING_DATA_REDIS_PORT`. Only basket references Redis.
- **Projects:** AppHost.
- **Verify:** AppHost build; `aspire run` shows redis healthy; basket env shows the two Redis vars; catalog and ordering have none.

### 2.3 Catalog schema and seed data
- Add Spring Data JPA, Flyway, PostgreSQL driver and Testcontainers (postgresql) to catalog; `spring.jpa.hibernate.ddl-auto=validate`.
- `db/migration/V1__create_product.sql` (§4.1 table) and `V2__seed_products.sql` (~24 products across 3–4 categories).
- `Product` entity + `ProductRepository` (no endpoints yet).
- **Projects:** Catalog.Api.
- **Verify:** `mvn test` — Testcontainers test that Flyway applies and the repository returns the seeded count; `aspire run` → catalog healthy (JPA validate passes); pgAdmin `SELECT category, count(*) FROM product GROUP BY 1` in catalogdb.

### 2.4 Ordering schema
- Add JPA, Flyway, PostgreSQL driver and Testcontainers to ordering; `ddl-auto=validate`.
- `V1__create_orders.sql`: `orders` (index on `buyer_id`) and `order_item` (FK → orders) (§4.3).
- `Order` / `OrderItem` entities and `OrderRepository`.
- **Projects:** Ordering.Api.
- **Verify:** `mvn test` — repository round-trip (save an order with items, read it back); `aspire run` → ordering healthy; pgAdmin shows both tables in orderingdb.

---

## Phase 3 — Microservices

### Catalog.Api

#### 3.1 Catalog read API
- `GET /products?category=&q=&page=0&size=8` → `{ items, page, size, totalItems, totalPages }`; `q` case-insensitive on name, `category` exact, ordered by name.
- `GET /products/{id}` → `Product` or 404.
- `GET /categories` → distinct sorted `string[]`.
- DTO records in `api`.
- **Projects:** Catalog.Api.
- **Verify:** `mvn test` (controller happy paths + filter/search/paging + 404, with Testcontainers); `aspire run`; HTTP via proxy: `/api/catalog/products?page=2`, `?category=…`, `?q=…`, `/api/catalog/products/1`, `/api/catalog/products/99999` (404), `/api/catalog/categories`; Trace: catalog → Postgres span for each request.

### Ordering.Api

#### 3.2 Create order
- `POST /orders` `{ buyerId, items[] }`: validate a non-empty item list and quantity > 0 (400 otherwise), compute `total`, set `created_at`, persist; 201 with `Order`.
- **Projects:** Ordering.Api.
- **Verify:** `mvn test` (201 + computed total; 400 on empty list / quantity 0); HTTP via proxy `POST /api/ordering/orders`; pgAdmin shows rows in `orders` and `order_item`.

#### 3.3 Query orders
- `GET /orders?buyerId=` → `OrderSummary[]` (`id, createdAt, total, itemCount`), newest first, not paged.
- `GET /orders/{id}` → `Order` with items, or 404.
- **Projects:** Ordering.Api.
- **Verify:** `mvn test` (newest-first ordering, itemCount, 404); HTTP via proxy for the order created in 3.2; Trace: ordering → Postgres.

### Basket.Api

#### 3.4 Basket storage (Redis only, no service calls)
- Spring Data Redis; key `basket:{buyerId}` holding JSON `{ buyerId, items[] }`, no TTL.
- `GET /basket/{buyerId}` → basket with computed `total` (empty basket if the key is missing).
- `PUT /basket/{buyerId}/items/{productId}` `{ quantity }` → set; `0` removes; 404 if the line is missing.
- **Projects:** Basket.Api.
- **Verify:** `mvn test` with Testcontainers Redis (empty basket, update, remove, 404); `aspire run`; HTTP via proxy `GET /api/basket/basket/<uuid>` returns an empty basket.

---

## Phase 4 — Service communication

#### 4.1 Basket → Catalog: add item with product lookup
- `client/CatalogClient` (Spring `RestClient`, base URL `catalog.url`) calling `GET /products/{id}`.
- `POST /basket/{buyerId}/items` `{ productId, quantity=1 }`: look up the product (404 if Catalog returns 404), snapshot name + price, add the line or increment it.
- **Projects:** Basket.Api.
- **Verify:** `mvn test` with `MockRestServiceServer` (add new line, increment existing, unknown product → 404); HTTP via proxy: add the same product twice plus another, then GET shows snapshot prices and total; Trace: basket → catalog → Postgres as one trace.

#### 4.2 Basket → Ordering: checkout
- `client/OrderingClient` (`RestClient`, base URL `ordering.url`) calling `POST /orders`.
- `POST /basket/{buyerId}/checkout`: read basket (400 if empty/missing) → POST `{ buyerId, items }` with snapshot prices → only after 201 delete `basket:{buyerId}` → 201 `{ orderId }`. An Ordering failure propagates as 5xx and leaves the basket intact (§5). No retries or compensation.
- **Projects:** Basket.Api.
- **Verify:** `mvn test` (success clears the basket; Ordering 500 → 5xx and basket kept; empty → 400); HTTP via proxy: fill basket, checkout, GET basket is empty, `GET /api/ordering/orders/{orderId}` matches; failure check: stop `ordering` in the dashboard, checkout → 5xx, basket unchanged; Trace: single checkout trace basket → ordering → Postgres.

---

## Phase 5 — Storefront pages

#### 5.1 App shell and shared basket state
- Header: app name → `/`, **My orders** → `/orders`, cart icon → `/cart` with badge = sum of quantities.
- Shared basket state (load `GET /basket/{buyerId}` on start, refresh after every basket call); error display for failed calls (errors propagate to the UI, §2).
- **Projects:** Storefront.
- **Verify:** `npm run build`; `aspire run`; Browser: header links navigate, badge shows the quantities of a basket filled via curl.

#### 5.2 Product catalog page `/`
- Product grid (vue-base components), category select (`/categories`), search box (`q`), pager (`page`, `size=8`), **Add to cart** per card → `POST /items`, badge updates.
- **Projects:** Storefront.
- **Verify:** Browser: filter, search and page through the ~24 products; add to cart updates the badge; network requests hit only `/api/catalog/*` and `/api/basket/*` (same origin).

#### 5.3 Cart page `/cart`
- Lines with +/- quantity and remove (`PUT /items/{productId}`, `0` removes), total, **Checkout** button → `/checkout` (disabled when empty).
- **Projects:** Storefront.
- **Verify:** Browser: change a quantity, remove a line; total and badge stay consistent.

#### 5.4 Checkout page `/checkout`
- Read-only lines and total; **Place order** disabled while the request runs → `POST /checkout` → refresh badge (0) → navigate to `/orders/{orderId}`; on failure show the error and keep the basket.
- **Projects:** Storefront.
- **Verify:** Browser: placing an order lands on the order detail with badge 0; a double-click creates one order (check `/orders`); with `ordering` stopped, the error is shown and the cart is intact.

#### 5.5 Orders pages `/orders` and `/orders/:id`
- `/orders`: this buyer's orders (date, item count, total), newest first, each linking to its detail.
- `/orders/:id`: lines, total, date.
- **Projects:** Storefront.
- **Verify:** Browser: the order from 5.4 is listed and its detail matches the snapshot prices; a new buyerId (cleared localStorage) shows no orders.

---

## Phase 6 — End-to-end verification

#### 6.1 Definition-of-done walkthrough (§10)
- Fresh start: stop Aspire, remove the postgres/redis volumes if persistent, `aspire run` from `src/`.
- Check each item:
  - [ ] `src/api` and `src/frontend` are gone; `apphost.mts` has no starter resources or `publishWithContainerFiles`.
  - [ ] postgres, pgAdmin, redis, catalog, basket, ordering and storefront are all healthy.
  - [ ] pgAdmin shows `catalogdb` (seeded) and `orderingdb`.
  - [ ] Browser journey: filter/search/page → add two products, one twice → change a quantity and remove a line → checkout → order detail with snapshot prices and correct total, badge 0 → `/orders` lists the order.
  - [ ] Dashboard shows one checkout trace: basket → ordering → Postgres.
  - [ ] `mvn test` passes in catalog, ordering and basket.
- Fix anything found on this branch; update `README.md` with how to run (prereqs: Docker, Node, Aspire CLI, JDK 21/Maven for tests).
- **Projects:** all (fixes only), README.
- **Verify:** every checkbox above ticked; a Playwright run of the journey with screenshots of the order detail and the checkout trace.
