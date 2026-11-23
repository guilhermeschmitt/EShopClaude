# ordering 04 — get order

**Status:** Done (df164a2)

## Goal
Return one order with its lines, for the storefront's order detail page (`/orders/:id`, PROJECT.md §6) and so basket/04 can confirm what checkout created.
It reuses the `Order` shape that `POST /orders` already returns (`OrderDto`), so the 201 body and the detail are the same JSON.

## Context
- ordering/01 (Done) wired `ordering` to `orderingdb` in `src/apphost.mts` and created `orders` and `order_item` (FK `order_id`, indexed) through Flyway `V1__create_orders.sql`.
- ordering/02 (Done) added `POST /orders` in `api/OrderController` (constructor-injected `OrderRepository` and `Clock`), the records `OrderDto(Long id, UUID buyerId, Instant createdAt, BigDecimal total, List<OrderItemDto> items)` and `OrderItemDto(Long productId, String productName, BigDecimal unitPrice, int quantity)` with `from(...)` factories, `OrderControllerTests` and `src/test/java/.../ordering/http/ordering.http`. Every stored order has at least one line.
- ordering/03 (Done) added `GET /orders?buyerId=` (`list`) to the same controller, the `repository.OrderSummary` projection, `OrderRepository.findSummariesByBuyerId` (`@Query`), `api/OrderSummaryDto` and `OrderListControllerTests`.
- Domain: `Order` has `@OneToMany(mappedBy = "order", cascade = ALL, orphanRemoval = true) List<OrderItem> items` with **no ordering**; `OrderItem.order` is `@ManyToOne(LAZY)`. `application.yml` sets `spring.jpa.open-in-view: false`, so a plain `findById` followed by `OrderDto.from` outside a transaction throws `LazyInitializationException` on `items`.
- Tests: every test class shares one Spring context and one `postgres:17-alpine` container (`TestcontainersConfiguration`), so tables are never empty; tests seed their own data and look it up by the generated id.
- The 404 pattern is catalog's `ProductController.get(@PathVariable long id)`: `findById(...).map(...).orElseThrow(() -> new ResponseStatusException(NOT_FOUND, ...))`.
- The AppHost needs no change. The pom needs no new dependency. No migration change.

## Depends on
- `ordering/02-create-order`

## Scope
- [ ] `GET /orders/{id}` returns 200 with `Order = { id, buyerId, createdAt, total, items[] }` (the existing `OrderDto`), where each item is `{ productId, productName, unitPrice, quantity }`.
- [ ] An unknown id (any number with no row, including `0` and negatives) returns 404 through `ResponseStatusException`, with Spring's default error JSON.
- [ ] A non-numeric id (`/orders/abc`) returns Spring's default 400 (`@PathVariable long id`, as in catalog).
- [ ] The order and its lines load in **one SELECT** through an `@EntityGraph(attributePaths = "items")` repository method, with no transaction in the controller.
- [ ] `items` come back in the order they were added: `@OrderBy("id asc")` on `Order.items`.
- [ ] Repository and controller tests for found-with-items (including line order), 404 and 400.
- [ ] Add the get requests to `ordering.http`.

## Explicit non-goals
- No update or delete of orders, and no order status (§9).
- No check that the order belongs to a buyer (no `buyerId` parameter): there is no auth by design (§4.3).
- No change to `POST /orders` or `GET /orders?buyerId=`, to `OrderDto`/`OrderItemDto`, the migrations, `application.yml` or `pom.xml`. The only domain change is the `@OrderBy` annotation.
- No `@ControllerAdvice` and no custom error body.
- No AppHost change, and no changes to catalog, basket or the storefront.

## Data model
No schema change.

**Entity** (`domain/Order.java`): add `@OrderBy("id asc")` (`jakarta.persistence.OrderBy`) to `items`. Mapping only; `ddl-auto=validate` is unaffected.

**Repository method** on `OrderRepository`:
```java
@EntityGraph(attributePaths = "items")
Optional<Order> findWithItemsById(Long id);
```
Derived query (`findWithItemsById` → `where o.id = ?`) with the `items` collection fetch-joined, so the returned `Order` has its lines initialized and ordered by `order_item.id`.

**Response DTO:** the existing `OrderDto` / `OrderItemDto`, unchanged. In JSON, `createdAt` is an ISO-8601 string, and `total` and `unitPrice` are numbers.

## Tasks
1. **Entity** (`domain/Order.java`, modified): add `@OrderBy("id asc")` on `items`.
2. **Repository** (`repository/OrderRepository.java`, modified): add `findWithItemsById` with `@EntityGraph(attributePaths = "items")` (`org.springframework.data.jpa.repository.EntityGraph`).
3. **Controller** (`api/OrderController.java`, modified): add
   ```java
   @GetMapping("/orders/{id}")
   public OrderDto get(@PathVariable long id)
   ```
   which returns `orderRepository.findWithItemsById(id).map(OrderDto::from)` or throws `new ResponseStatusException(HttpStatus.NOT_FOUND, "order " + id + " not found")`. Leave `create` and `list` as they are.
4. **Repository tests** (`src/test/java/.../ordering/repository/OrderRepositoryTests.java`, modified): add `findWithItemsByIdLoadsLinesInInsertionOrder`. Save an order for a random buyer with lines added as Tee (id 2) ×1, then Mug (id 1) ×2, then Cap (id 3) ×4 (deliberately not sorted by productId or name). Call `findWithItemsById(id)` **outside** any transaction; `getItems()` is accessible (no `LazyInitializationException`) and has productIds `2, 1, 3` in exactly that order. Also add `findWithItemsByIdUnknownIsEmpty`: `findWithItemsById(Long.MAX_VALUE)` is empty.
5. **Controller tests** (`src/test/java/.../ordering/api/OrderDetailControllerTests.java`, new): `@SpringBootTest` + `@AutoConfigureMockMvc` + `@Import(TestcontainersConfiguration.class)`. Seed through `OrderRepository.save` with a random buyer and an explicit `Instant`.
   - `returnsOrderWithItems`: an order at `2026-02-01T10:00:00Z` with Tee 19.50×1 then Mug 9.90×2. `GET /orders/{id}` returns 200 with `$.id` = id, `$.buyerId` = the buyer, `$.createdAt` = `"2026-02-01T10:00:00Z"`, `$.total` = 39.3, `$.items` of size 2, `$.items[0]` = `{productId 2, productName "Tee", unitPrice 19.5, quantity 1}` and `$.items[1]` = `{productId 1, productName "Mug", unitPrice 9.9, quantity 2}`.
   - `unknownIdIsNotFound`: `GET /orders/9223372036854775807` returns 404.
   - `nonNumericIdIsBadRequest`: `GET /orders/abc` returns 400.
6. **Manual requests** (`src/test/java/.../ordering/http/ordering.http`, modified): add `GET /orders/1` (expect 200 with items, if the create request above ran first), `GET /orders/999999999` (expect 404) and `GET /orders/abc` (expect 400).

## Files touched
- `src/java-back/ordering/src/main/java/br/com/guilherme/eshop/ordering/domain/Order.java` (modified)
- `src/java-back/ordering/src/main/java/br/com/guilherme/eshop/ordering/repository/OrderRepository.java` (modified)
- `src/java-back/ordering/src/main/java/br/com/guilherme/eshop/ordering/api/OrderController.java` (modified)
- `src/java-back/ordering/src/test/java/br/com/guilherme/eshop/ordering/repository/OrderRepositoryTests.java` (modified)
- `src/java-back/ordering/src/test/java/br/com/guilherme/eshop/ordering/api/OrderDetailControllerTests.java` (new)
- `src/java-back/ordering/src/test/java/br/com/guilherme/eshop/ordering/http/ordering.http` (modified)

Nothing changes in `src/apphost.mts`, `pom.xml`, `application.yml`, the migrations, `OrderDto`/`OrderItemDto`, nor in catalog, basket or the storefront.

## Definition of Done
Run every check from the repository root unless it says otherwise. `<ordering-port>` is ordering's HTTP port from the Aspire dashboard. Run the curl commands in Git Bash.

1. **AppHost build:** in `src/`, `npm run build` exits 0 with no eslint or tsc errors.
2. **Tests:** with Docker running, `mvn -f src/java-back/ordering/pom.xml test` prints `BUILD SUCCESS`. The report lists `OrderingApplicationTests`, `OrderRepositoryTests`, `OrderControllerTests`, `OrderListControllerTests` and `OrderDetailControllerTests` with 0 failures and 0 errors.
3. **Image:** `docker build src/java-back/ordering` exits 0.
4. **Healthy start:** in `src/`, `aspire run` shows every resource (postgres, catalogdb, orderingdb, pgAdmin, redis, catalog, basket, ordering) as Running/Healthy.
5. **Create an order:** run
   ```
   curl -s -X POST http://localhost:<ordering-port>/orders -H 'Content-Type: application/json' \
     -d '{"buyerId":"33333333-3333-3333-3333-333333333333","items":[{"productId":2,"productName":"Tee","unitPrice":19.50,"quantity":1},{"productId":1,"productName":"Mug","unitPrice":9.90,"quantity":2}]}'
   ```
   It returns a 201 body. Note its `id` as `<id>` and keep the body.
6. **Get the order:** `curl -s -i http://localhost:<ordering-port>/orders/<id>` returns `HTTP/1.1 200` and the body `{"id":<id>,"buyerId":"33333333-3333-3333-3333-333333333333","createdAt":"<same as step 5>","total":39.3 (or 39.30),"items":[{"productId":2,"productName":"Tee","unitPrice":19.5 (or 19.50),"quantity":1},{"productId":1,"productName":"Mug","unitPrice":9.9 (or 9.90),"quantity":2}]}`: the same fields and values as the step 5 body, with the items in the order they were sent.
7. **Unknown id → 404:** `curl -s -o /dev/null -w '%{http_code}' http://localhost:<ordering-port>/orders/999999999` prints `404`.
8. **Non-numeric id → 400:** `curl -s -o /dev/null -w '%{http_code}' http://localhost:<ordering-port>/orders/abc` prints `400`.
9. **List still works:** `curl -s "http://localhost:<ordering-port>/orders?buyerId=33333333-3333-3333-3333-333333333333"` returns a JSON array whose entry with `"id":<id>` has `"itemCount":3`.
10. **Trace:** in the Aspire dashboard, Traces filtered on the `ordering` resource shows a `GET /orders/{id}` trace from check 6 with exactly one child PostgreSQL `SELECT` span on `orderingdb` (order and lines in one query).

## References
- PROJECT.md §4.3 Ordering (`GET /orders/{id}` → `Order = { id, buyerId, createdAt, total, items[] }`, or 404; no auth by design), §6 Storefront (`/orders/:id` page: lines, total and date), §8 Java service conventions (`api` controllers and DTO records, Testcontainers controller happy paths), §9 (no order status or lifecycle).
- PLAN.md Phase 3 → Ordering, spec 4 (`04-get-order.md`). It replaces the old PLAN 3.2–3.3 for the detail endpoint; it feeds basket/04 (checkout DoD reads `GET /orders/{orderId}`) and Phase 5.5.
- Pattern: catalog `ProductController.get` (404 via `ResponseStatusException`, `@PathVariable long id`), ordering's `OrderDto.from` and `OrderListControllerTests`.
- Prior specs: `microservices/ordering/02-create-order.md` and `03-list-buyer-orders.md` (Done).
