# EShopClaude — Build Plan

Source of truth: [PROJECT.md](PROJECT.md). This plan orders the work; PROJECT.md defines the behavior.
Section refs like (§4.1) point to PROJECT.md.

## How to use this plan
- Branching, merging and the per-spec cycle follow [WOW.md](WOW.md).
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

### 2.1 Data infrastructure in the AppHost
Infrastructure only: no reference, waitFor or env var between the APIs and these resources (that is 2.2), and no Java project changes (the Spring data starters are added in 2.3, 2.4 and 3.4, together with the wiring they need to start).
- **2.1.1** Add `Aspire.Hosting.PostgreSQL` and `Aspire.Hosting.Redis` (same version as the other Aspire packages) to `packages` in `aspire.config.json`; regenerate `src/.aspire/modules/` and confirm `addPostgres`, `addDatabase`, `withPgAdmin` and `addRedis` exist in `aspire.mts`.
- **2.1.2** `postgres` server resource with the `catalogdb` and `orderingdb` databases.
- **2.1.3** `withPgAdmin()` on `postgres`.
- **2.1.4** `redis` resource, no data volume.
- **Projects:** AppHost.
- **Verify:** AppHost build; `aspire run` shows postgres, catalogdb, orderingdb, pgAdmin and redis healthy, and catalog, basket, ordering still healthy; catalog, basket and ordering have no `ConnectionStrings__*`, `SPRING_DATASOURCE_*` or `SPRING_DATA_REDIS_*` env vars; pgAdmin lists `catalogdb` and `orderingdb`; `redis-cli ping` in the redis container returns `PONG` and no volume is mounted.

Steps 2.2–2.4 (wiring each service to its data store, its schema and the catalog seed) moved to the service specs in Phase 3.

---

## Phase 3 — Microservices

Each service is built as a series of small specs, one feature branch each ([WOW.md](WOW.md)). The specs hold the scope and the Definition of Done; the status table is in [microservices/README.md](microservices/README.md).

**Build order:** specs run in number order inside a service. Catalog, Ordering and basket 01 don't depend on each other. basket 02 needs catalog 04, and basket 04 needs ordering 02 and 04.

### Catalog
- **Owns:** products in `catalogdb` and the read-only product API: a filtered, searchable, paged list, a product by id, and the categories (§4.1).
- **Specs:** [microservices/catalog/](microservices/catalog/)
  1. `01-product-schema.md`
  2. `02-seed-products.md`
  3. `03-list-products.md`
  4. `04-get-product.md`
  5. `05-list-categories.md`

### Ordering
- **Owns:** orders in `orderingdb`. It persists and serves orders, and never calls another service (§4.3).
- **Specs:** [microservices/ordering/](microservices/ordering/)
  1. `01-order-schema.md`
  2. `02-create-order.md`
  3. `03-list-buyer-orders.md`
  4. `04-get-order.md`

### Basket
- **Owns:** baskets in Redis and the checkout flow. It calls Catalog when an item is added and Ordering at checkout (§4.2, §5).
- **Specs:** [microservices/basket/](microservices/basket/)
  1. `01-read-basket.md`
  2. `02-add-item.md`
  3. `03-update-item-quantity.md`
  4. `04-checkout.md`

Where the old steps went: 2.2 → catalog 01, ordering 01, basket 01; 2.3 → catalog 01–02; 2.4 → ordering 01; 3.1 → catalog 03–05; 3.2–3.3 → ordering 02–04; 3.4 → basket 01 and 03; Phase 4 (service communication) → basket 02 (4.1) and basket 04 (4.2).

---

## Phase 5 — Storefront

The storefront is built like the services: a series of small specs, one feature branch each ([WOW.md](WOW.md)). A storefront spec changes only `src/vue-front/storefront/` and the `storefront` registration in the AppHost. Its pages call the three services but never change them. The status table is in [microservices/README.md](microservices/README.md).

- **Owns:** the browser UI, the anonymous `buyerId` and the Vite proxy to the services (§2, §3, §6).
- **References:** the first spec whose pages call a service adds that service's `services__<name>__http__0` + `waitFor` and its `/api/<name>` proxy entry: basket in 02, catalog in 03, ordering in 11.
- **Specs:** [microservices/storefront/](microservices/storefront/)
  1. `01-storefront-shell.md`
  2. `02-cart-badge.md`
  3. `03-product-grid.md`
  4. `04-product-pager.md`
  5. `05-category-filter.md`
  6. `06-product-search.md`
  7. `07-add-to-cart.md`
  8. `08-cart-view.md`
  9. `09-cart-adjust.md`
  10. `10-checkout-review.md`
  11. `11-my-orders.md`
  12. `12-order-detail.md`
  13. `13-place-order.md`

Where the old steps went: 1.3 and the storefront part of 1.4 → storefront 01 (the storefront was never built in Phase 1); 5.1 → storefront 02; 5.2 → storefront 03–07; 5.3 → storefront 08–09; 5.4 → storefront 10 and 13; 5.5 → storefront 11–12.

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
