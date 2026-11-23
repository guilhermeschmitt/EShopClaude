# ordering 03 — list buyer orders

**Status:** Done (fb347c9)

## Goal
List a buyer's orders as summaries, newest first, for the storefront's "My orders" page (`/orders`, PROJECT.md §6).
This is the first read endpoint in Ordering. It reads the orders that ordering/02 writes, without loading their lines.

## Context
- ordering/01 (Done) wired `ordering` to `orderingdb` in `src/apphost.mts` and created `orders` (indexed on `buyer_id`) and `order_item` (FK `order_id`, indexed) through Flyway `V1__create_orders.sql`. `quantity` is `int not null check (quantity > 0)`, and `created_at` is `timestamptz`.
- ordering/02 (Done) added `POST /orders` in `api/OrderController` (constructor-injected `OrderRepository` and `Clock`; `createdAt` truncated to microseconds), the records `CreateOrderRequest`, `CreateOrderItem`, `OrderDto` and `OrderItemDto` (with `from(...)` factories), `config/ClockConfig` (`Clock.systemUTC()`), `OrderControllerTests` and `src/test/java/.../ordering/http/ordering.http`. Validation guarantees that every stored order has at least one line.
- Domain: `Order(UUID buyerId, Instant createdAt)` with `addItem(...)`, getters, and `@OneToMany(mappedBy = "order") items`. `OrderItem` has a `@ManyToOne(LAZY) order`. `repository.OrderRepository extends JpaRepository<Order, Long>` and has no query methods yet.
- Tests: every test class shares one Spring context and one `postgres:17-alpine` container (`TestcontainersConfiguration`), so tables are never empty and new tests must use their own random `buyerId`.
- The pattern for a repository query is catalog's `ProductRepository.findDistinctCategories()` (`@Query` JPQL), and for a list endpoint it is catalog's `CategoryController` (returns a `List` directly).
- The AppHost needs no change. The pom needs no new dependency.

## Depends on
- `ordering/02-create-order`

## Scope
- [ ] `GET /orders?buyerId={uuid}` returns 200 with `OrderSummaryDto[] = { id, createdAt, total, itemCount }`, ordered by `createdAt` descending, then `id` descending. It is not paged.
- [ ] `itemCount` is the **sum of the quantities** of the order's lines (Mug×2 + Tee×1 → 3), the same as the cart badge (§6).
- [ ] `buyerId` is a required `@RequestParam UUID`: a missing or non-UUID value returns Spring's default 400. A buyer with no orders returns `[]`.
- [ ] One JPQL query with a constructor projection, so neither the order lines nor the entities are loaded (no N+1).
- [ ] Tests for newest-first ordering, `itemCount`, isolation between buyers, an unknown buyer and the 400 cases.
- [ ] Add the list requests to `ordering.http`.

## Explicit non-goals
- No paging, no sorting parameters and no filters other than `buyerId` (§9: no order history paging).
- No order detail: `GET /orders/{id}` is ordering/04.
- No change to `POST /orders`, the domain entities, the migrations, `application.yml` or `pom.xml`.
- No `@ControllerAdvice` and no custom error body: Spring's default error JSON, as in ordering/02.
- No AppHost change, and no changes to catalog, basket or the storefront.

## Data model
No schema change.

**Repository projection** (package `repository`, so the repository does not depend on `api`):

| Record | Fields |
|---|---|
| `OrderSummary` | `Long id`, `Instant createdAt`, `BigDecimal total`, `Long itemCount` |

**Query** on `OrderRepository`:
```java
@Query("""
    select new br.com.guilherme.eshop.ordering.repository.OrderSummary(o.id, o.createdAt, o.total, sum(i.quantity))
    from Order o join o.items i
    where o.buyerId = :buyerId
    group by o.id, o.createdAt, o.total
    order by o.createdAt desc, o.id desc
    """)
List<OrderSummary> findSummariesByBuyerId(@Param("buyerId") UUID buyerId);
```
An inner join is enough because every order has at least one line. JPQL `sum` over an `int` returns `Long`.

**Response DTO** (package `api`):

| Record | Fields |
|---|---|
| `OrderSummaryDto` | `Long id`, `Instant createdAt`, `BigDecimal total`, `long itemCount` |

`OrderSummaryDto.from(OrderSummary)` is a static factory, as in `OrderDto`. In JSON, `createdAt` is an ISO-8601 string, and `total` and `itemCount` are numbers.

## Tasks
1. **Projection** (`repository/OrderSummary.java`, new): the record from Data model.
2. **Repository** (`repository/OrderRepository.java`, modified): add `findSummariesByBuyerId` with the `@Query` above.
3. **DTO** (`api/OrderSummaryDto.java`, new): the record with `from(OrderSummary)`.
4. **Controller** (`api/OrderController.java`, modified): add `@GetMapping("/orders") public List<OrderSummaryDto> list(@RequestParam UUID buyerId)`, which maps `orderRepository.findSummariesByBuyerId(buyerId)` through `OrderSummaryDto::from`. Leave `create` as it is.
5. **Repository tests** (`src/test/java/.../ordering/repository/OrderRepositoryTests.java`, modified): add `findSummariesByBuyerIdOrdersNewestFirstAndSumsQuantities`. For a random buyer, save three orders with explicit `createdAt` values (`2026-01-01T10:00:00Z` with Mug×2 + Tee×1, `2026-01-02T10:00:00Z` with one line ×1, `2026-01-03T10:00:00Z` with one line ×5). Also save one order for a second random buyer. The result has 3 summaries, the 03 → 02 → 01 orders in that sequence, with `itemCount` values 5, 1, 3. It also checks the `total` with `isEqualByComparingTo` and does not include the other buyer's order.
6. **Controller tests** (`src/test/java/.../ordering/api/OrderListControllerTests.java`, new): `@SpringBootTest` + `@AutoConfigureMockMvc` + `@Import(TestcontainersConfiguration.class)`. Seed the data through `OrderRepository.save` with explicit `Instant`s and a fresh `UUID.randomUUID()` buyer per test (never through a clock).
   - `listsBuyerOrdersNewestFirst`: two orders for the buyer (`2026-01-01T10:00:00Z` Mug 9.90×2 + Tee 19.50×1, and `2026-01-02T10:00:00Z` Mug 9.90×1) plus one for another buyer. `GET /orders?buyerId=<buyer>` returns 200 with `$` of size 2. `$[0].id` is the newer order, `$[0].createdAt` is `"2026-01-02T10:00:00Z"`, `$[0].total` is 9.9 and `$[0].itemCount` is 1. `$[1].total` is 39.3 and `$[1].itemCount` is 3. No entry has an `items` field.
   - `sameCreatedAtTieBreaksOnIdDescending`: two orders with the same `createdAt` are listed higher `id` first.
   - `unknownBuyerReturnsEmptyList`: a random UUID returns 200 and `[]`.
   - `missingBuyerIdIsBadRequest`: `GET /orders` returns 400.
   - `invalidBuyerIdIsBadRequest`: `GET /orders?buyerId=not-a-uuid` returns 400.
7. **Manual requests** (`src/test/java/.../ordering/http/ordering.http`, modified): add `GET /orders?buyerId=11111111-1111-1111-1111-111111111111` (expect 200, newest first), an unknown buyer (expect `[]`), a missing `buyerId` (expect 400) and `buyerId=not-a-uuid` (expect 400).

## Files touched
- `src/java-back/ordering/src/main/java/br/com/guilherme/eshop/ordering/repository/OrderSummary.java` (new)
- `src/java-back/ordering/src/main/java/br/com/guilherme/eshop/ordering/repository/OrderRepository.java` (modified)
- `src/java-back/ordering/src/main/java/br/com/guilherme/eshop/ordering/api/OrderSummaryDto.java` (new)
- `src/java-back/ordering/src/main/java/br/com/guilherme/eshop/ordering/api/OrderController.java` (modified)
- `src/java-back/ordering/src/test/java/br/com/guilherme/eshop/ordering/repository/OrderRepositoryTests.java` (modified)
- `src/java-back/ordering/src/test/java/br/com/guilherme/eshop/ordering/api/OrderListControllerTests.java` (new)
- `src/java-back/ordering/src/test/java/br/com/guilherme/eshop/ordering/http/ordering.http` (modified)

Nothing changes in `src/apphost.mts`, `pom.xml`, `application.yml`, the migrations or the domain entities, nor in catalog, basket or the storefront.

## Definition of Done
Run every check from the repository root unless it says otherwise. `<ordering-port>` is ordering's HTTP port from the Aspire dashboard. Run the curl commands in Git Bash. `<buyer>` is a fresh UUID, for example `22222222-2222-2222-2222-$(date +%s%N | cut -c1-12)`, used only in this walk-through.

1. **AppHost build:** in `src/`, `npm run build` exits 0 with no eslint or tsc errors.
2. **Tests:** with Docker running, `mvn -f src/java-back/ordering/pom.xml test` prints `BUILD SUCCESS`. The report lists `OrderingApplicationTests`, `OrderRepositoryTests`, `OrderControllerTests` and `OrderListControllerTests` with 0 failures and 0 errors.
3. **Image:** `docker build src/java-back/ordering` exits 0.
4. **Healthy start:** in `src/`, `aspire run` shows every resource (postgres, catalogdb, orderingdb, pgAdmin, redis, catalog, basket, ordering) as Running/Healthy.
5. **Two orders:** run
   ```
   curl -s -X POST http://localhost:<ordering-port>/orders -H 'Content-Type: application/json' \
     -d '{"buyerId":"<buyer>","items":[{"productId":1,"productName":"Mug","unitPrice":9.90,"quantity":2},{"productId":2,"productName":"Tee","unitPrice":19.50,"quantity":1}]}'
   ```
   then
   ```
   curl -s -X POST http://localhost:<ordering-port>/orders -H 'Content-Type: application/json' \
     -d '{"buyerId":"<buyer>","items":[{"productId":3,"productName":"Cap","unitPrice":5.00,"quantity":4}]}'
   ```
   Each returns a 201 body. Note the ids as `<first>` and `<second>`.
6. **List, newest first:** `curl -s -i "http://localhost:<ordering-port>/orders?buyerId=<buyer>"` returns `HTTP/1.1 200` and a JSON array of exactly 2 entries. Entry 0 is `{"id":<second>, "createdAt":"<ISO-8601>", "total":20.0 (or 20.00), "itemCount":4}`, and entry 1 is `{"id":<first>, …, "total":39.3 (or 39.30), "itemCount":3}`. Entry 0's `createdAt` is later than entry 1's, and neither entry has an `items` field.
7. **Unknown buyer:** `curl -s -i "http://localhost:<ordering-port>/orders?buyerId=$(uuidgen 2>/dev/null || echo 99999999-9999-9999-9999-999999999999)"` returns `HTTP/1.1 200` and the body `[]`.
8. **Missing buyerId → 400:** `curl -s -o /dev/null -w '%{http_code}' http://localhost:<ordering-port>/orders` prints `400`.
9. **Invalid buyerId → 400:** `curl -s -o /dev/null -w '%{http_code}' "http://localhost:<ordering-port>/orders?buyerId=not-a-uuid"` prints `400`.
10. **Trace:** in the Aspire dashboard, Traces filtered on the `ordering` resource shows a `GET /orders` trace from check 6 with a child PostgreSQL span on `orderingdb` (one `SELECT` for the summaries).

## References
- PROJECT.md §4.3 Ordering (`GET /orders?buyerId={uuid}` → `OrderSummary[] = { id, createdAt, total, itemCount }`, newest first, not paged), §6 Storefront (`/orders` page: date, item count, total; cart badge = sum of quantities), §8 Java service conventions (`api` controllers and DTO records, Testcontainers controller happy paths), §9 (no order history paging).
- PLAN.md Phase 3 → Ordering, spec 3 (`03-list-buyer-orders.md`). It replaces the old PLAN 3.2–3.3 for the list endpoint, and it feeds Phase 5.5.
- Pattern: catalog `ProductRepository.findDistinctCategories()` (`@Query`), `CategoryController` (list endpoint), and ordering's `OrderDto.from` and `OrderControllerTests`.
- Prior spec: `microservices/ordering/02-create-order.md` (Done).
