# ordering 02 — create order

**Status:** Not started

## Goal
Persist an order sent by Basket at checkout. Ordering trusts the prices it receives and computes the total.
This is the write side that basket/04 (checkout) calls, and it gives ordering/03 and ordering/04 real orders to read. It also adds the `OrderDto` response record that ordering/04 reuses.

## Context
- ordering/01 (Done) wired `ordering` to `orderingdb` in `src/apphost.mts` (`SPRING_DATASOURCE_*`, `withReference` + `waitFor`). It also created `orders` and `order_item` through Flyway `V1__create_orders.sql`, with check constraints `total >= 0`, `unit_price >= 0` and `quantity > 0`, `product_name varchar(100)`, and `numeric(10,2)` money columns.
- The domain is ready. `domain.Order` has a public constructor `Order(UUID buyerId, Instant createdAt)` that starts with `total = 0.00`, and `addItem(long productId, String productName, BigDecimal unitPrice, int quantity)` adds `unitPrice × quantity` to `total`. It also has getters and an unmodifiable `getItems()`. `domain.OrderItem` has getters `getId/getProductId/getProductName/getUnitPrice/getQuantity`, and its constructor is package-private. `repository.OrderRepository extends JpaRepository<Order, Long>`.
- The `api` package holds only `RootController` (`GET /` returns plain text). There is no controller, no DTO and no `Clock` bean yet.
- Tests: `TestcontainersConfiguration` (a `@ServiceConnection` `postgres:17-alpine`), `OrderingApplicationTests` (root + health through MockMvc) and `OrderRepositoryTests` (3 tests). Every test class shares one Spring context and one container, so new tests must not assume empty tables.
- The pom already has `spring-boot-starter-webmvc`, data-jpa, flyway, the postgres driver, `spring-boot-starter-webmvc-test`, `spring-boot-testcontainers` and the Testcontainers postgresql/junit-jupiter modules. **No new dependencies.** There is no `spring-boot-starter-validation`; validation is written by hand.
- The pattern to copy is catalog's `api` layer. `ProductController` validates by hand and throws `ResponseStatusException(HttpStatus.BAD_REQUEST, reason)`, and `ProductDto` is a record with a static `from(entity)` factory. `ProductControllerTests` uses `@SpringBootTest` + `@AutoConfigureMockMvc` (from `org.springframework.boot.webmvc.test.autoconfigure`) + `@Import(TestcontainersConfiguration.class)`. There is a manual-requests file `src/test/java/.../http/catalog.http`.
- The AppHost needs no change. ordering is already registered, healthy and connected to orderingdb.

## Depends on
- `ordering/01-order-schema`

## Scope
- [ ] `POST /orders` with body `CreateOrderRequest { buyerId, items: [{ productId, productName, unitPrice, quantity }] }`.
- [ ] Validation by hand: every rule in Data model → *Validation* returns 400 and persists nothing. Malformed JSON and a non-UUID `buyerId` also return 400 (Spring's default).
- [ ] Build the `Order` with `createdAt = Instant.now(clock)`, add each line through `Order.addItem` (so `total = Σ unitPrice × quantity`), save it, and return **201** with `OrderDto`. No `Location` header.
- [ ] `Clock` bean (`Clock.systemUTC()`) in a small config class.
- [ ] Request and response DTO records in `api`.
- [ ] Tests: 201 with the computed total and a pinned `createdAt`, rows persisted, 400 for each validation rule with nothing persisted.
- [ ] Manual requests file `ordering.http`.

## Explicit non-goals
- No query endpoints: `GET /orders?buyerId=` is ordering/03 and `GET /orders/{id}` is ordering/04.
- No calls to Catalog and no re-pricing. Prices are trusted as sent.
- No order status, no checkout logic and no idempotency key (the storefront prevents double-submit, §5).
- No `Location` header and no order-item id in the response.
- No `spring-boot-starter-validation`, no `@ControllerAdvice` and no custom error body. Spring's default error JSON is used, as in catalog.
- No schema change (no new migration) and no AppHost change.
- No changes to catalog, basket or the storefront.

## Data model
No schema change: the `orders` and `order_item` tables from ordering/01 are used as they are.

**DTO records** (package `api`):

| Record | Fields |
|---|---|
| `CreateOrderRequest` | `UUID buyerId`, `List<CreateOrderItem> items` |
| `CreateOrderItem` | `Long productId`, `String productName`, `BigDecimal unitPrice`, `Integer quantity` |
| `OrderDto` | `Long id`, `UUID buyerId`, `Instant createdAt`, `BigDecimal total`, `List<OrderItemDto> items` |
| `OrderItemDto` | `Long productId`, `String productName`, `BigDecimal unitPrice`, `int quantity` |

- The request uses boxed types (`Long` and `Integer`) so a missing field arrives as `null` and can be rejected, instead of silently becoming `0`.
- `OrderDto.from(Order)` and `OrderItemDto.from(OrderItem)` are static factories, as in catalog's `ProductDto`. Items keep the request order.
- JSON: `createdAt` is an ISO-8601 string (for example `"2026-01-15T10:00:00Z"`), and `total` and `unitPrice` are JSON numbers.

**Validation** (each rule → 400 via `ResponseStatusException(BAD_REQUEST, reason)`, checked before anything is saved):

1. `buyerId` is `null`.
2. `items` is `null` or empty.
3. Any item is `null`.
4. `productId` is `null`.
5. `productName` is `null`, blank, or longer than 100 characters.
6. `unitPrice` is `null`, `< 0`, has more than 2 decimal places (`stripTrailingZeros().scale() > 2`), or is greater than `99999999.99`, the largest `numeric(10,2)` value.
7. `quantity` is `null` or `<= 0`.
8. The computed total is greater than `99999999.99`.

The rules on name length, decimal places and the `numeric(10,2)` maximum mirror the database. Without them a bad input would fail as a 500, or be rounded so that the 201 total differs from the stored one.

## Tasks
1. **Clock** (`config/ClockConfig.java`, new package `br.com.guilherme.eshop.ordering.config`): `@Configuration(proxyBeanMethods = false)` with `@Bean Clock clock()` returning `Clock.systemUTC()`.
2. **DTOs** (`api`): the four records from Data model, with the `from(...)` factories on `OrderDto` and `OrderItemDto`.
3. **Controller** (`api/OrderController.java`):
   - `@RestController`, with `OrderRepository` and `Clock` injected through the constructor.
   - `@PostMapping("/orders")` taking `@RequestBody CreateOrderRequest` and returning `ResponseEntity<OrderDto>` with status 201.
   - Steps: validate (a private method that applies the Data model rules in order), build `new Order(buyerId, Instant.now(clock))`, call `addItem(...)` per line, check the total against the maximum, `orderRepository.save(order)`, then return `OrderDto.from(saved)`.
   - Remove nothing from `RootController`.
4. **Tests** (`src/test/java/.../ordering/api/OrderControllerTests.java`, new):
   - `@SpringBootTest` + `@AutoConfigureMockMvc` + `@Import(TestcontainersConfiguration.class)`.
   - Pin the clock with `@TestBean` (`org.springframework.test.context.bean.override.convention.TestBean`) on a `Clock clock` field, with a static factory that returns `Clock.fixed(Instant.parse("2026-01-15T10:00:00Z"), ZoneOffset.UTC)`.
   - Because the context and container are shared, check persistence with `orderRepository.count()` before and after, never absolute counts.
   - The tests:
     - `createsOrderAndComputesTotal`: POST buyer `11111111-1111-1111-1111-111111111111` with `{1, "Mug", 9.90, 2}` and `{2, "Tee", 19.50, 1}` → 201. `$.id` is a number, `$.buyerId` is the UUID, `$.createdAt` = `"2026-01-15T10:00:00Z"`, `$.total` = 39.30, `$.items` has 2 entries in request order with the same fields, and there is no `Location` header. `orderRepository.count()` grew by 1. In a `TransactionTemplate`, `findById($.id)` has total `39.30` (`isEqualByComparingTo`) and 2 items.
     - `emptyItemsIsBadRequest`: `"items": []` → 400.
     - `missingItemsIsBadRequest`: no `items` → 400.
     - `zeroQuantityIsBadRequest`: quantity `0` → 400.
     - `negativeQuantityIsBadRequest`: quantity `-1` → 400.
     - `invalidFieldsAreBadRequest`: a `@ParameterizedTest` over request bodies (a `@MethodSource` of JSON strings), one per remaining rule: missing `buyerId`, a `null` item, missing `productId`, missing/blank/101-character `productName`, missing/`-0.01`/`9.999`/`100000000.00` `unitPrice`, missing `quantity`, and an overflowing total (`unitPrice 99999999.99`, `quantity 2`). Each returns 400.
     - `malformedBodyIsBadRequest`: a non-UUID `buyerId` and broken JSON each return 400.
     - `rejectedRequestsPersistNothing`: across several of the invalid bodies above, `orderRepository.count()` is unchanged.
5. **Manual requests** (`src/test/java/.../ordering/http/ordering.http`, new): written like `catalog.http`, with `@baseUrl`, root, health, a valid `POST /orders` (expect 201, total 39.30), and one request per main 400 case (empty items, quantity 0, negative price, 3-decimal price).

## Files touched
- `src/java-back/ordering/src/main/java/br/com/guilherme/eshop/ordering/config/ClockConfig.java` (new)
- `src/java-back/ordering/src/main/java/br/com/guilherme/eshop/ordering/api/CreateOrderRequest.java` (new)
- `src/java-back/ordering/src/main/java/br/com/guilherme/eshop/ordering/api/CreateOrderItem.java` (new)
- `src/java-back/ordering/src/main/java/br/com/guilherme/eshop/ordering/api/OrderDto.java` (new)
- `src/java-back/ordering/src/main/java/br/com/guilherme/eshop/ordering/api/OrderItemDto.java` (new)
- `src/java-back/ordering/src/main/java/br/com/guilherme/eshop/ordering/api/OrderController.java` (new)
- `src/java-back/ordering/src/test/java/br/com/guilherme/eshop/ordering/api/OrderControllerTests.java` (new)
- `src/java-back/ordering/src/test/java/br/com/guilherme/eshop/ordering/http/ordering.http` (new)

Nothing changes in `src/apphost.mts`, `pom.xml`, `application.yml` or the migrations, nor in catalog, basket or the storefront.

## Definition of Done
Run every check from the repository root unless it says otherwise. `<ordering-port>` is ordering's HTTP port from the Aspire dashboard. `<psql>` is `docker exec -i <postgres-container> sh -c 'PGPASSWORD="$POSTGRES_PASSWORD" psql -U postgres -d orderingdb'`, with the SQL piped on stdin, as in ordering/01. Run the curl commands in Git Bash.

1. **AppHost build:** in `src/`, `npm run build` exits 0 with no eslint or tsc errors.
2. **Tests:** with Docker running, `mvn -f src/java-back/ordering/pom.xml test` prints `BUILD SUCCESS`. The report lists `OrderingApplicationTests`, `OrderRepositoryTests` and `OrderControllerTests` with 0 failures and 0 errors.
3. **Image:** `docker build src/java-back/ordering` exits 0.
4. **Healthy start:** in `src/`, `aspire run` shows every resource (postgres, catalogdb, orderingdb, pgAdmin, redis, catalog, basket, ordering) as Running/Healthy.
5. **Create:**
   ```
   curl -s -i -X POST http://localhost:<ordering-port>/orders -H 'Content-Type: application/json' \
     -d '{"buyerId":"11111111-1111-1111-1111-111111111111","items":[{"productId":1,"productName":"Mug","unitPrice":9.90,"quantity":2},{"productId":2,"productName":"Tee","unitPrice":19.50,"quantity":1}]}'
   ```
   returns `HTTP/1.1 201`, no `Location` header, and a JSON body with a numeric `id`, `buyerId` `11111111-1111-1111-1111-111111111111`, an ISO-8601 `createdAt`, `total` `39.3` (or `39.30`) and 2 `items` echoing the lines sent. Note the `id` as `<id>`.
6. **Rows persisted:** `echo "select buyer_id, total from orders where id = <id>" | <psql>` returns one row, `11111111-1111-1111-1111-111111111111 | 39.30`, and `echo "select product_id, product_name, unit_price, quantity from order_item where order_id = <id> order by id" | <psql>` returns `1 | Mug | 9.90 | 2` and `2 | Tee | 19.50 | 1`.
7. **Empty list → 400:** the same curl with `"items":[]` returns `HTTP/1.1 400`.
8. **Quantity 0 → 400:** the create curl with the first line's `"quantity":0` returns `HTTP/1.1 400`.
9. **Price precision → 400:** the create curl with `"unitPrice":9.999` returns `HTTP/1.1 400`.
10. **Nothing persisted on 400:** after checks 7–9, `echo "select count(*) from orders" | <psql>` returns the same count as right after check 5.
11. **pgAdmin:** orderingdb → Schemas → public → Tables → `orders` and `order_item` → View/Edit Data shows the order from check 5 and its two lines.

## References
- PROJECT.md §4.3 Ordering (the `POST /orders` contract, validation of a non-empty list and quantity > 0, `total = Σ unitPrice×quantity`, trusting Basket's prices), §5 Checkout flow (steps 3–4: Basket POSTs `{ buyerId, items }` and gets 201 `{ id, ... }`), §8 Java service conventions (`api` layer of controllers and DTO records, Testcontainers controller happy paths), §9 (no re-pricing, no order status).
- PLAN.md Phase 3 → Ordering, spec 2 (`02-create-order.md`). It replaces the old PLAN 3.2–3.3 for the create endpoint.
- Pattern: `catalog/src/main/java/.../api/ProductController.java` (manual 400 checks), `ProductDto.java` (record with `from`), `ProductControllerTests.java` (MockMvc + Testcontainers) and `catalog.http`.
- Prior spec: `microservices/ordering/01-order-schema.md` (Done).
