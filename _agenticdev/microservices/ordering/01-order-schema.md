# ordering 01 — order schema

**Status:** Not started

## Goal
Give Ordering its own database: wire `ordering` to `orderingdb` in the AppHost and create the `orders` and `order_item` tables through Flyway, with the entities and repository later specs build on.
The wiring and the schema are kept in one spec because neither can be verified alone. Spring ignores `SPRING_DATASOURCE_*` until JPA and the driver are on the classpath, and JPA cannot start under Aspire without those variables.

## Context
- This is the first Ordering spec. Today `ordering` is registered in the shared `for (const name of ['basket', 'ordering'])` loop in `src/apphost.mts` (endpoint 8080, `/health`, OTLP, cert trust) with no data references. `orderingdb` already exists (`await postgres.addDatabase('orderingdb')`), but its result is not kept in a variable and no service is wired to it.
- catalog/01 already did the same wiring for catalog. Its registration in `src/apphost.mts` is the pattern to copy: `withReference` + `waitFor` on the database, and `SPRING_DATASOURCE_URL` from `jdbcConnectionString()`, `SPRING_DATASOURCE_USERNAME` from `postgres.userNameReference()` and `SPRING_DATASOURCE_PASSWORD` from `postgres.passwordParameter.get()`. ordering runs as a container, so the JDBC host and port must be the container-network values that Aspire resolves, not a hand-written `localhost` URL.
- The ordering service has no JPA, Flyway or PostgreSQL driver dependencies. Its only code is `RootController`, the empty `domain` and `repository` packages (`package-info.java`) and the context-load test `OrderingApplicationTests`.
- catalog's `pom.xml`, `application.yml` and `TestcontainersConfiguration` (on `postgres:17-alpine`) are the working Boot 4 reference for the dependency names and test setup.
- Unlike catalog's read-only `Product`, orders are written by ordering/02, so the entities need a constructor that builds a new order and its items.
- Spring Boot 4 starter names differ from Boot 3 (see `CLAUDE.md`).

## Depends on
Nothing.

## Scope
- [ ] AppHost: take `ordering` out of the shared loop; reference + waitFor `orderingdb`.
- [ ] AppHost: build `SPRING_DATASOURCE_URL` (`jdbc:postgresql://host:port/orderingdb`), `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` from the postgres endpoint and parameters.
- [ ] pom: Spring Data JPA, Flyway (+ PostgreSQL module), PostgreSQL driver, Testcontainers (postgresql).
- [ ] `spring.jpa.hibernate.ddl-auto=validate`.
- [ ] `db/migration/V1__create_orders.sql`: `orders` (index on `buyer_id`) and `order_item` (FK → orders).
- [ ] `Order` and `OrderItem` entities, `OrderRepository`.
- [ ] Testcontainers repository round-trip test: save an order with items, read it back.

## Explicit non-goals
- No endpoints and no DTOs (ordering/02–04).
- No request validation beyond the database constraints (the 400 rules are ordering/02).
- No order status, buyer name, email or address.
- No calls to other services.
- No changes to catalog or basket registrations, and no Redis.

## Data model
Two tables, created by `V1__create_orders.sql`. The table is `orders` (plural) because `order` is a reserved SQL word.

Table `orders`:

| Column | Type | Null |
|---|---|---|
| `id` | `bigserial` primary key | no |
| `buyer_id` | `uuid` | no |
| `created_at` | `timestamptz` | no |
| `total` | `numeric(10,2)` with `CHECK (total >= 0)` | no |

Plus the index `ix_orders_buyer_id` on `(buyer_id)`.

Table `order_item`:

| Column | Type | Null |
|---|---|---|
| `id` | `bigserial` primary key | no |
| `order_id` | `bigint`, FK `fk_order_item_order` → `orders(id)`, no `ON DELETE` action | no |
| `product_id` | `bigint` | no |
| `product_name` | `varchar(100)` | no |
| `unit_price` | `numeric(10,2)` with `CHECK (unit_price >= 0)` | no |
| `quantity` | `int` with `CHECK (quantity > 0)` | no |

Plus the index `ix_order_item_order_id` on `(order_id)`. Orders are never deleted in this project, so the FK has no cascade. There is no FK to Catalog's `product`: it lives in another database, and the name and price are a snapshot.

- **Entity** `domain.Order`:
  - `@Entity` and `@Table(name = "orders")`.
  - `Long id` with `@GeneratedValue(strategy = IDENTITY)`.
  - `UUID buyerId`, `Instant createdAt` and `BigDecimal total` (`precision = 10`, `scale = 2`), with `@Column` names and nullability that match the table.
  - `List<OrderItem> items` with `@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)`, initialised to an empty list.
  - A protected no-arg constructor for JPA, and a public constructor `Order(UUID buyerId, Instant createdAt)` that starts with `total = 0.00`. The caller passes `createdAt`, so ordering/02 can use a `Clock` and tests stay deterministic.
  - `addItem(long productId, String productName, BigDecimal unitPrice, int quantity)` creates the `OrderItem`, links it to this order, adds it to `items` and adds `unitPrice × quantity` to `total`. This is the only way to add an item, so `total` always equals `Σ unitPrice × quantity`.
  - Getters only; `getItems()` returns an unmodifiable view. No setters and no Lombok.
- **Entity** `domain.OrderItem`:
  - `@Entity` and `@Table(name = "order_item")`.
  - `Long id` with `@GeneratedValue(strategy = IDENTITY)`.
  - `Order order` with `@ManyToOne(fetch = LAZY, optional = false)` and `@JoinColumn(name = "order_id", nullable = false)`.
  - `Long productId`, `String productName` (`length = 100`), `BigDecimal unitPrice` (`precision = 10`, `scale = 2`) and `int quantity`, all `nullable = false`.
  - A protected no-arg constructor for JPA, and a package-private constructor used only by `Order.addItem`. Getters only, with no getter for `order` (it isn't needed and would invite lazy-loading surprises).
- **Repository** `repository.OrderRepository extends JpaRepository<Order, Long>`, with no custom queries (ordering/03 adds the buyer query).
- **How the schema is created:** Flyway only. It runs when ordering starts. `spring.jpa.hibernate.ddl-auto=validate` then checks the entities against the tables. Hibernate's validation doesn't check CHECK constraints, indexes or FK names, so the Definition of Done verifies those with `psql`.

## Tasks
1. **AppHost** (`src/apphost.mts`):
   - Keep the result of `postgres.addDatabase('orderingdb')` in an `orderingdb` variable.
   - Change the shared loop to `['basket']` (or register basket on its own with the identical calls), which leaves basket unchanged. Register `ordering` on its own with the same `addDockerfile('ordering', './java-back/ordering')`, `withHttpEndpoint({ targetPort: 8080 })`, `withHttpHealthCheck({ path: '/health' })`, `withOtlpExporter()` and `withCertificateTrustEnvironment('OTEL_EXPORTER_OTLP_CERTIFICATE')`.
   - Add `withReference(orderingdb)` and `waitFor(orderingdb)`.
   - Add `withEnvironment` for:
     - `SPRING_DATASOURCE_URL` = `await orderingdb.jdbcConnectionString()`
     - `SPRING_DATASOURCE_USERNAME` = `await postgres.userNameReference()`
     - `SPRING_DATASOURCE_PASSWORD` = `postgres.passwordParameter.get()`
   - Update the comment above the loop so it no longer mentions ordering/01.
   - Every builder call is awaited (`no-floating-promises`).
2. **pom** (`src/java-back/ordering/pom.xml`): add the same dependencies as `src/java-back/catalog/pom.xml`, with no hard-coded versions:
   - Main: `spring-boot-starter-data-jpa`, `spring-boot-starter-flyway`, `org.flywaydb:flyway-database-postgresql`, and `org.postgresql:postgresql` with runtime scope.
   - Test: `spring-boot-testcontainers`, `org.testcontainers:testcontainers-postgresql` and `org.testcontainers:testcontainers-junit-jupiter`.
3. **`application.yml`**: add `spring.jpa.hibernate.ddl-auto: validate` and `spring.jpa.open-in-view: false`, with the same comment as catalog's. It holds no datasource values; those come from the environment (AppHost) or from `@ServiceConnection` (tests).
4. **Migration** `src/main/resources/db/migration/V1__create_orders.sql`: both tables, the check constraints, the FK and both indexes from Data model. Create `orders` first.
5. **Code**: `domain/Order.java`, `domain/OrderItem.java` and `repository/OrderRepository.java` as described in Data model. Delete the placeholder `package-info.java` in `domain` and `repository` once each package has a real class.
6. **Tests**:
   - A shared `TestcontainersConfiguration` (`@TestConfiguration(proxyBeanMethods = false)`) with a `@ServiceConnection` `PostgreSQLContainer` bean on `postgres:17-alpine`, the same as catalog's.
   - Add `@Import(TestcontainersConfiguration.class)` to the existing `OrderingApplicationTests`. Without a datasource it fails once JPA is on the classpath.
   - New `OrderRepositoryTests` (`@SpringBootTest` + `@Import(TestcontainersConfiguration.class)`):
     - `flywayAppliedV1`: Flyway's current migration is version `1` with state `SUCCESS`.
     - `savesOrderWithItemsAndReadsItBack`: build `new Order(buyerId, createdAt)`, `addItem(1, "Mug", 9.90, 2)` and `addItem(2, "Tee", 19.50, 1)`, save it, then in a new transaction (`TransactionTemplate`) `findById` returns an order with the same `buyerId`, the same `createdAt` (compare truncated to microseconds), `total` = `39.30` (`isEqualByComparingTo`), and 2 items with the product ids, names, unit prices and quantities sent.
     - `totalIsSumOfLines`: a new order has `total` 0; after the two `addItem` calls above, `total` is `39.30` without saving.

## Files touched
- `src/apphost.mts` (edit)
- `src/java-back/ordering/pom.xml` (edit)
- `src/java-back/ordering/src/main/resources/application.yml` (edit)
- `src/java-back/ordering/src/main/resources/db/migration/V1__create_orders.sql` (new)
- `src/java-back/ordering/src/main/java/br/com/guilherme/eshop/ordering/domain/Order.java` (new)
- `src/java-back/ordering/src/main/java/br/com/guilherme/eshop/ordering/domain/OrderItem.java` (new)
- `src/java-back/ordering/src/main/java/br/com/guilherme/eshop/ordering/domain/package-info.java` (delete)
- `src/java-back/ordering/src/main/java/br/com/guilherme/eshop/ordering/repository/OrderRepository.java` (new)
- `src/java-back/ordering/src/main/java/br/com/guilherme/eshop/ordering/repository/package-info.java` (delete)
- `src/java-back/ordering/src/test/java/br/com/guilherme/eshop/ordering/TestcontainersConfiguration.java` (new)
- `src/java-back/ordering/src/test/java/br/com/guilherme/eshop/ordering/OrderingApplicationTests.java` (edit)
- `src/java-back/ordering/src/test/java/br/com/guilherme/eshop/ordering/repository/OrderRepositoryTests.java` (new)

Nothing in catalog, basket or the storefront changes.

## Definition of Done
Run every check from the repository root unless it says otherwise. `<postgres-container>` is the postgres container name from `docker ps`, and `<ordering-port>` is ordering's HTTP port from the dashboard. `<psql>` is `docker exec -i <postgres-container> sh -c 'PGPASSWORD="$POSTGRES_PASSWORD" psql -U postgres -d orderingdb'`, with the SQL piped on stdin (Aspire's Postgres asks for a password even inside its own container).

1. **AppHost build:** in `src/`, `npm run build` exits 0 with no eslint or tsc errors.
2. **Tests:** with Docker running, `mvn -f src/java-back/ordering/pom.xml test` prints `BUILD SUCCESS`, and the report lists `OrderingApplicationTests` and `OrderRepositoryTests` (3 tests) with 0 failures and 0 errors.
3. **Image:** `docker build src/java-back/ordering` exits 0.
4. **Healthy start:** in `src/`, `aspire run` shows every resource (postgres, catalogdb, orderingdb, pgAdmin, redis, catalog, basket, ordering) as Running/Healthy in the dashboard. On a fresh start, ordering shows *Waiting* until orderingdb is healthy.
5. **ordering env:** dashboard → ordering → details → Environment shows:
   - `SPRING_DATASOURCE_URL`, starting with `jdbc:postgresql://` and ending with `/orderingdb`
   - `SPRING_DATASOURCE_USERNAME` = `postgres`
   - `SPRING_DATASOURCE_PASSWORD`, set and not empty
6. **No leaks to other services:** dashboard → basket → Environment shows no `SPRING_DATASOURCE_*` and no `ConnectionStrings__orderingdb`; dashboard → catalog → Environment still ends its `SPRING_DATASOURCE_URL` with `/catalogdb` and has no `ConnectionStrings__orderingdb`.
7. **Migration ran:** dashboard → ordering → Console logs contain Flyway's `Successfully applied 1 migration to schema "public", now at version v1`.
8. **DB health:** `curl http://localhost:<ordering-port>/health` returns `{"status":"UP"}`.
9. **`orders` shape:** `echo '\d orders' | <psql>` shows the four columns with the types and nullability from Data model, `orders_pkey`, `ix_orders_buyer_id`, a check constraint on `total >= 0`, and `order_item` listed under *Referenced by*.
10. **`order_item` shape:** `echo '\d order_item' | <psql>` shows the six columns with the types and nullability from Data model, `order_item_pkey`, `ix_order_item_order_id`, check constraints on `unit_price >= 0` and `quantity > 0`, and `fk_order_item_order` referencing `orders(id)`.
11. **Migration history:** `echo "select version, success from flyway_schema_history" | <psql>` returns exactly one row, `1 | t`.
12. **Empty tables:** `echo "select (select count(*) from orders), (select count(*) from order_item)" | <psql>` returns `0 | 0`.
13. **Quantity constraint:** `echo "insert into order_item(order_id, product_id, product_name, unit_price, quantity) values (1, 1, 'x', 1, 0)" | <psql>` fails with `violates check constraint`.
14. **FK constraint:** `echo "insert into order_item(order_id, product_id, product_name, unit_price, quantity) values (999, 1, 'x', 1, 1)" | <psql>` fails with `violates foreign key constraint "fk_order_item_order"`.
15. **pgAdmin:** orderingdb → Schemas → public → Tables lists `orders`, `order_item` and `flyway_schema_history`.

## References
- PROJECT.md §4.3 Ordering (the `orders` and `order_item` tables, `V1__create_orders.sql`, no status or buyer details), §7 Aspire AppHost (ordering → orderingdb, the `SPRING_DATASOURCE_*` env contract), §8 Java service conventions (layers, JPA, Flyway, `ddl-auto=validate`, Testcontainers), §9 (no order status or lifecycle).
- PLAN.md Phase 3 → Ordering, spec 1 (`01-order-schema.md`). It replaces the old PLAN 2.2 and 2.4 wiring for ordering.
- Pattern: `microservices/catalog/01-product-schema.md` (Done).
