# EShopClaude — Project Document

## 1. Purpose
EShopClaude is a minimal e-commerce demo that shows agentic coding with microservices. It has a small scope, uses few tokens and ships fast. It is built on the Aspire starter in `src/`.

## 2. Architecture
```
Browser ──► storefront (Vue 3 + Vite, vue-base)
               │  Vite dev proxy (URLs from Aspire service discovery)
               ├─ /api/catalog/*  ─► catalog  ──► Postgres: catalogdb
               ├─ /api/basket/*   ─► basket   ──► Redis (baskets)
               │                       ├─HTTP─► catalog  (product lookup on add)
               │                       └─HTTP─► ordering (create order on checkout)
               └─ /api/ordering/* ─► ordering ──► Postgres: orderingdb

pgAdmin ──► Postgres (catalogdb, orderingdb)
```
- **Services:** catalog, basket and ordering use Java 21, Spring Boot and Maven, with plain Spring Boot starters and no shared base library. Each one runs in a Docker container built from its own Dockerfile.
- **Storefront:** Vue 3 with Vite and vue-base. There is no Blazor. The starter's `src/api` and `src/frontend` are deleted.
- **Data:** Catalog and Ordering share one PostgreSQL server, and each has its own database: `catalogdb` and `orderingdb`. Basket stores baskets in Redis. pgAdmin is attached to the PostgreSQL server so the data in both databases can be browsed.
- **Communication:** synchronous HTTP only, through Aspire service discovery. There is no gateway, no broker and no events.
- **Browser access:** the browser only talks to the storefront origin. `vite.config` proxies `/api/{service}` to the URL that Aspire injects for each referenced service (`services__{name}__http__0`), with the `/api/{service}` prefix stripped. No CORS is needed.
- **Resilience:** Aspire and Spring defaults only. There are no retries, circuit breakers, fallbacks or compensation. Errors propagate to the UI.
- **Observability:** each Dockerfile adds the OpenTelemetry Java agent. Aspire's injected `OTEL_EXPORTER_OTLP_*` and `OTEL_SERVICE_NAME` env vars send traces, logs and metrics to the Aspire dashboard. The checkout trace basket → ordering → Postgres is the showcase. The storefront (browser and Vite proxy) is not instrumented, so traces start at Basket.

## 3. Identity without login
- On first load the storefront generates `buyerId` (UUID v4) and stores it in `localStorage` under `eshop.buyerId`.
- `buyerId` identifies the basket (`/basket/{buyerId}`) and tags every order. It is the only notion of a "customer".
- There is no checkout form and no name, email or address. Clearing browser storage gives a new, empty identity.

## 4. Services

### 4.1 Catalog (owns products, `catalogdb`)
**Data:** `product(id bigserial PK, name varchar(100) not null, description varchar(500), price numeric(10,2) not null, category varchar(50) not null)`. The category is a plain column, with no category table. The Flyway migrations are `V1__create_product.sql` and `V2__seed_products.sql`. Flyway runs when Catalog starts, so `V2` seeds about 24 sample products across 3–4 categories on first start, which makes paging visible.

**API** (read-only, with no admin CRUD):

| Method | Path | Result |
|---|---|---|
| GET | `/products?category=&q=&page=0&size=8` | `{ items: Product[], page, size, totalItems, totalPages }`. `q` is a case-insensitive match on name. `category` is an exact match. Results are ordered by name. |
| GET | `/products/{id}` | `Product`, or 404 |
| GET | `/categories` | `string[]`, the distinct categories sorted |

`Product = { id, name, description, price, category }`

### 4.2 Basket (owns baskets in Redis, orchestrates checkout)
**Data:** Redis key `basket:{buyerId}` holds a JSON `{ buyerId, items: [{ productId, productName, unitPrice, quantity }] }` with no TTL. Nothing is stored in Postgres.

**What it stores and what it looks up:** the client sends only `productId` and `quantity`. On add, Basket calls Catalog `GET /products/{id}`, which validates that the product exists (otherwise 404) and returns the authoritative name and price. Basket stores that snapshot. Reading the basket never calls Catalog. **The price shown in the basket is the price charged**, so checkout does not re-price.

**API:**

| Method | Path | Behavior |
|---|---|---|
| GET | `/basket/{buyerId}` | `{ buyerId, items[], total }`. A missing key returns an empty basket. `total` is computed and never stored. |
| POST | `/basket/{buyerId}/items` `{ productId, quantity=1 }` | Looks up Catalog, then adds the line or increments it. Returns the basket. |
| PUT | `/basket/{buyerId}/items/{productId}` `{ quantity }` | Sets the quantity, and `0` removes the line. Returns the basket, or 404 if the line doesn't exist. |
| POST | `/basket/{buyerId}/checkout` | Runs the checkout flow (§5). Returns 201 `{ orderId }`, or 400 if the basket is empty. |

### 4.3 Ordering (owns orders, `orderingdb`)
Ordering stays simple: it only exposes CRUD operations for orders and has no checkout or orchestration logic. It never calls another service.

**Data:** the Flyway migration `V1__create_orders.sql` creates two tables:
- `orders(id bigserial PK, buyer_id uuid not null, created_at timestamptz not null, total numeric(10,2) not null)`, indexed on `buyer_id`
- `order_item(id bigserial PK, order_id FK → orders, product_id bigint, product_name varchar(100), unit_price numeric(10,2), quantity int)`

An order has no status field and no buyer name, email or address.

**API:**

| Method | Path | Behavior |
|---|---|---|
| POST | `/orders` `{ buyerId, items: [{ productId, productName, unitPrice, quantity }] }` | Validates a non-empty list and quantities > 0, computes `total = Σ unitPrice×quantity`, and persists. Returns 201 with the `Order`. It trusts the prices Basket sends and never calls Catalog. |
| GET | `/orders?buyerId={uuid}` | `OrderSummary[] = { id, createdAt, total, itemCount }`, newest first and not paged |
| GET | `/orders/{id}` | `Order = { id, buyerId, createdAt, total, items[] }`, or 404 |

Accepted demo tradeoff: `POST /orders` can be reached through the storefront proxy. There is no auth by design.

## 5. Checkout flow (owned by Basket)
Basket owns all checkout logic: a checkout request reads the basket, asks Ordering to create the order, and deletes the basket once the order exists. When Basket needs product data, it calls Catalog. Checkout itself needs none, because the basket already holds the snapshot taken on add (§4.2).

1. The user clicks **Place order** on `/checkout`. The storefront disables the button while the request runs, so a double-click can't create two orders, and calls `POST /api/basket/basket/{buyerId}/checkout`.
2. Basket reads `basket:{buyerId}` from Redis. If it is empty or missing, Basket returns 400.
3. Basket POSTs `{ buyerId, items }` to Ordering `/orders`, using the snapshot prices.
4. Ordering persists the order and returns 201 `{ id, ... }`.
5. Only after that 201 does Basket delete `basket:{buyerId}` and return 201 `{ orderId }`.
6. The storefront refreshes the cart badge, which is now 0, and navigates to `/orders/{orderId}`.

If any step fails, the error propagates as a 5xx, the basket stays intact and the user can retry. There is no compensation.

## 6. Storefront (Vue 3 + vue-base)
- **Setup:** `Core.init(app, router, routes, { baseURL: '/api' })`. Every route is public, so vue-base's guards must not require auth. The UI uses vue-base components and its bundled axios.
- **Header:** the app name links to `/`, there is a **My orders** link to `/orders`, and a cart icon links to `/cart` with an item-count badge (the sum of quantities).

| Route | Page | Calls |
|---|---|---|
| `/` | Product grid with a category select, a search box, a pager and **Add to cart** on each card | catalog `GET /products`, `GET /categories`; basket `POST /items` |
| `/cart` | Lines with quantity +/- and remove, the total and a **Checkout** button | basket `GET`, `PUT /items/{productId}` |
| `/checkout` | Read-only review of lines and total, with a **Place order** button that is disabled while the request runs | basket `GET`, `POST /checkout` |
| `/orders` | This buyer's orders (date, item count, total), each linking to its detail | ordering `GET /orders?buyerId=` |
| `/orders/:id` | Order detail: lines, total and date | ordering `GET /orders/{id}` |

**Journey:** browse/filter → add → cart (adjust) → checkout review → place order → order detail → My orders.

## 7. Aspire AppHost (`src/apphost.mts`)
- **Starter cleanup:** delete the starter's `src/api` and `src/frontend` folders. In `apphost.mts`, remove the starter's `app` (`addNodeApp`) and `frontend` (`addViteApp`) resources and the `publishWithContainerFiles(...)` call, because publishing is out of scope.
- Add the packages `Aspire.Hosting.PostgreSQL` and `Aspire.Hosting.Redis` to `aspire.config.json`.
- Resources:
  - `postgres` with the databases `catalogdb` and `orderingdb`, and pgAdmin attached through `withPgAdmin()`
  - `redis`
  - `catalog`, `basket` and `ordering` through `addDockerfile`, with an HTTP endpoint on container port 8080
  - `storefront` through `addViteApp("./storefront")`, with external HTTP endpoints
- References and waits:
  - catalog → catalogdb
  - ordering → orderingdb
  - basket → redis, catalog and ordering
  - storefront → catalog, basket and ordering
- Only Basket references Redis. Catalog and Ordering never connect to it.
- **Env contract for the Java services:** Catalog and Ordering get `SPRING_DATASOURCE_URL` (`jdbc:postgresql://…/catalogdb` or `…/orderingdb`), `SPRING_DATASOURCE_USERNAME` and `SPRING_DATASOURCE_PASSWORD`. Basket gets `SPRING_DATA_REDIS_HOST` and `SPRING_DATA_REDIS_PORT`. The AppHost builds these from the resource endpoints and parameters, because Aspire's default connection-string format is not JDBC. Basket also reads `services__catalog__http__0` and `services__ordering__http__0`, mapped in `application.yml` to `catalog.url` and `ordering.url`.

## 8. Java service conventions (each of catalog, basket and ordering)
- **Project:** a standalone Maven project at `src/{service}/` with `spring-boot-starter-parent` as its parent. The base package is `br.com.guilherme.eshop.{service}`, with the layers `api` (controllers and DTO records), `domain`, `repository` and `client` (Basket only, using Spring `RestClient`).
- **Security:** no Spring Security dependency, so every endpoint is open. There is no login.
- **Database:** Catalog and Ordering use Spring Data JPA. Flyway migrations live in `src/main/resources/db/migration`, and `spring.jpa.hibernate.ddl-auto=validate`. Basket uses only Spring Data Redis and has no JPA or JDBC dependencies.
- **Dockerfile:** a multi-stage build.
  1. A build stage (Maven with JDK 21) runs `mvn package` on the service.
  2. A runtime stage (JRE 21) downloads `opentelemetry-javaagent.jar` and starts with `-javaagent`.
- **Tests:** JUnit 5 and Spring Boot Test.
  - Catalog and Ordering use Testcontainers with PostgreSQL to cover the repository and controller happy paths.
  - Basket uses Testcontainers with Redis, with Catalog and Ordering stubbed through `MockRestServiceServer`. It covers add (with lookup), updating the quantity, and checkout (which clears the basket only after success).

## 9. Deliberately out of scope
- Login, identity, auth, payments, shipping and deployment or publishing.
- Product admin CRUD and product images.
- Stock and inventory, order status or lifecycle, and a checkout form (name, email, address).
- Re-pricing at checkout or detecting price drift.
- Retries, fallbacks, circuit breakers, sagas, compensation, an API gateway, a message broker and events.
- Basket TTL or expiry, order history paging, and frontend tests.

## 10. Definition of done
- The starter's `src/api` and `src/frontend` are gone, and `apphost.mts` has no starter resources and no `publishWithContainerFiles` call.
- `aspire run` from `src/` starts postgres, pgAdmin, redis, catalog, basket, ordering and storefront, all healthy in the dashboard.
- pgAdmin shows both `catalogdb` (with the seeded products) and `orderingdb`.
- Storefront journey:
  1. Filter by category, search and page through products.
  2. Add two different products, and add one of them twice.
  3. Change a quantity in the cart and remove a line.
  4. On checkout, the order detail page shows the snapshot prices and the correct total, and the cart badge drops to 0.
  5. `/orders` lists the order.
- The dashboard shows a single distributed trace for checkout: basket → ordering → Postgres.
- `mvn test` passes in each Java service, with Docker running for Testcontainers.
