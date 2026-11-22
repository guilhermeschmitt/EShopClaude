# catalog 01 — product schema

**Status:** Done

## Goal
Give Catalog its own database: wire `catalog` to `catalogdb` in the AppHost and create the empty `product` table through Flyway, with the `Product` entity and repository that later specs build on.
The wiring and the schema are kept in one spec because neither can be verified alone. Spring ignores `SPRING_DATASOURCE_*` until JPA and the driver are on the classpath, so a wrong JDBC URL would go unnoticed. And JPA cannot start under Aspire without those variables.

## Context
- This is the first Catalog spec. Today `catalog` is registered in the shared `for` loop in `src/apphost.mts` (endpoint 8080, `/health`, OTLP, cert trust) with no data references. `postgres`, `catalogdb` and `orderingdb` exist since PLAN 2.1, but no service is wired to them.
- The catalog service has no JPA, Flyway or PostgreSQL driver dependencies. Its only code is `RootController` and the context-load test `CatalogApplicationTests`.
- The AppHost is TypeScript, so its API comes from `src/.aspire/modules/aspire.mts`, not the C# docs. The helpers this spec uses all exist there: `PostgresDatabaseResource.jdbcConnectionString()`, `PostgresServerResource.userNameReference()` and `PostgresServerResource.passwordParameter`.
- catalog runs as a container, so the JDBC host and port must be the container-network values that Aspire resolves. That's why the URL comes from `jdbcConnectionString()` and is not a hand-written `localhost` URL.
- Spring Boot 4 starter names differ from Boot 3 (see `CLAUDE.md`).

## Depends on
Nothing.

## Scope
- [ ] AppHost: take `catalog` out of the shared loop; reference + waitFor `catalogdb`.
- [ ] AppHost: build `SPRING_DATASOURCE_URL` (`jdbc:postgresql://host:port/catalogdb`), `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` from the postgres endpoint and parameters (check `src/.aspire/modules/aspire.mts` for the helpers).
- [ ] pom: Spring Data JPA, Flyway (+ PostgreSQL module), PostgreSQL driver, Testcontainers (postgresql).
- [ ] `spring.jpa.hibernate.ddl-auto=validate`.
- [ ] `db/migration/V1__create_product.sql` with the `product` table.
- [ ] `Product` entity and `ProductRepository`.
- [ ] Testcontainers test: Flyway applies and the repository count is 0.

## Explicit non-goals
- No seed data (catalog/02).
- No new endpoints.
- No Redis, and no changes to basket or ordering registrations.

## Data model
Table `product`, created by `V1__create_product.sql`:

| Column | Type | Null |
|---|---|---|
| `id` | `bigserial` primary key | no |
| `name` | `varchar(100)` | no |
| `description` | `varchar(500)` | yes |
| `price` | `numeric(10,2)` with `CHECK (price >= 0)` | no |
| `category` | `varchar(50)` | no |

Plus the index `ix_product_category` on `(category)`. There is no category table; the category is a plain column (PROJECT.md §4.1).

- **Entity** `domain.Product`, a read-only class:
  - `@Entity` and `@Table(name = "product")`.
  - `Long id` with `@GeneratedValue(strategy = IDENTITY)`.
  - `String name`, `description` and `category`, with `@Column` lengths and nullability that match the table.
  - `BigDecimal price` with `precision = 10` and `scale = 2`.
  - A protected no-arg constructor for JPA, getters only, no setters and no Lombok.
- **Repository** `repository.ProductRepository extends JpaRepository<Product, Long>`, with no custom queries.
- **How the schema is created:** Flyway only. It runs when catalog starts. `spring.jpa.hibernate.ddl-auto=validate` then checks the entity against the table. Hibernate's validation doesn't check CHECK constraints or indexes, so the Definition of Done verifies those with `psql`.

## Tasks
1. **AppHost** (`src/apphost.mts`):
   - Keep the result of `postgres.addDatabase('catalogdb')` in a `catalogdb` variable.
   - Remove `catalog` from the shared loop, which keeps `basket` and `ordering` unchanged. Register `catalog` on its own with the same `addDockerfile`, `withHttpEndpoint({ targetPort: 8080 })`, `withHttpHealthCheck({ path: '/health' })`, `withOtlpExporter()` and `withCertificateTrustEnvironment('OTEL_EXPORTER_OTLP_CERTIFICATE')`.
   - Add `withReference(catalogdb)` and `waitFor(catalogdb)`.
   - Add `withEnvironment` for:
     - `SPRING_DATASOURCE_URL` = `catalogdb.jdbcConnectionString()`
     - `SPRING_DATASOURCE_USERNAME` = `postgres.userNameReference()`
     - `SPRING_DATASOURCE_PASSWORD` = `postgres.passwordParameter`
   - Every builder call is awaited (`no-floating-promises`).
2. **pom** (`src/java-back/catalog/pom.xml`):
   - Main: the Spring Data JPA starter, the Flyway starter (Boot 4 name), `org.flywaydb:flyway-database-postgresql`, and `org.postgresql:postgresql` with runtime scope.
   - Test: `spring-boot-testcontainers` and the Testcontainers PostgreSQL and JUnit Jupiter modules.
   - Take the versions from the dependency management of the `spring-boot-starter-parent` 4.2.0 parent. Check the exact artifact names there, and don't hard-code versions.
3. **`application.yml`**: add `spring.jpa.hibernate.ddl-auto: validate` and `spring.jpa.open-in-view: false`. It holds no datasource values; those come from the environment (AppHost) or from `@ServiceConnection` (tests).
4. **Migration** `src/main/resources/db/migration/V1__create_product.sql`: the table, check constraint and index from Data model.
5. **Code**: `domain/Product.java` and `repository/ProductRepository.java` as described in Data model.
6. **Tests**:
   - A shared `TestcontainersConfiguration` (`@TestConfiguration(proxyBeanMethods = false)`) with a `@ServiceConnection` `PostgreSQLContainer` bean on a pinned `postgres` image tag.
   - Add `@Import(TestcontainersConfiguration.class)` to the existing `CatalogApplicationTests`. Without a datasource it fails once JPA is on the classpath.
   - New `ProductRepositoryTests`: Flyway reports version `1` as applied and successful, and `productRepository.count()` is `0`.

## Files touched
- `src/apphost.mts` (edit)
- `src/java-back/catalog/pom.xml` (edit)
- `src/java-back/catalog/src/main/resources/application.yml` (edit)
- `src/java-back/catalog/src/main/resources/db/migration/V1__create_product.sql` (new)
- `src/java-back/catalog/src/main/java/br/com/guilherme/eshop/catalog/domain/Product.java` (new)
- `src/java-back/catalog/src/main/java/br/com/guilherme/eshop/catalog/repository/ProductRepository.java` (new)
- `src/java-back/catalog/src/test/java/br/com/guilherme/eshop/catalog/TestcontainersConfiguration.java` (new)
- `src/java-back/catalog/src/test/java/br/com/guilherme/eshop/catalog/CatalogApplicationTests.java` (edit)
- `src/java-back/catalog/src/test/java/br/com/guilherme/eshop/catalog/repository/ProductRepositoryTests.java` (new)

Nothing in basket, ordering or the storefront changes.

## Definition of Done
Run every check from the repository root unless it says otherwise. `<postgres-container>` is the postgres container name from `docker ps`, and `<catalog-port>` is catalog's HTTP port from the dashboard. `<psql>` is `docker exec -i <postgres-container> sh -c 'PGPASSWORD="$POSTGRES_PASSWORD" psql -U postgres -d catalogdb'`, with the SQL piped on stdin (Aspire's Postgres asks for a password even inside its own container).

1. **AppHost build:** in `src/`, `npm run build` exits 0 with no eslint or tsc errors.
2. **Tests:** with Docker running, `mvn -f src/java-back/catalog/pom.xml test` prints `BUILD SUCCESS`, and the report lists `CatalogApplicationTests` and `ProductRepositoryTests` with 0 failures and 0 errors.
3. **Image:** `docker build src/java-back/catalog` exits 0.
4. **Healthy start:** in `src/`, `aspire run` shows every resource (postgres, catalogdb, orderingdb, pgAdmin, redis, catalog, basket, ordering) as Running/Healthy in the dashboard. On a fresh start, catalog shows *Waiting* until catalogdb is healthy.
5. **catalog env:** dashboard → catalog → details → Environment shows:
   - `SPRING_DATASOURCE_URL`, starting with `jdbc:postgresql://` and ending with `/catalogdb`
   - `SPRING_DATASOURCE_USERNAME` = `postgres`
   - `SPRING_DATASOURCE_PASSWORD`, set and not empty
6. **No leaks to other services:** dashboard → basket and ordering → details → Environment show no `SPRING_DATASOURCE_*` and no `ConnectionStrings__catalogdb`.
7. **Migration ran:** dashboard → catalog → Console logs contain Flyway's `Successfully applied 1 migration to schema "public", now at version v1`.
8. **DB health:** `curl http://localhost:<catalog-port>/health` returns `{"status":"UP"}`. The datasource health indicator is included now that JPA is on the classpath.
9. **Table shape:** `echo '\d product' | <psql>` shows:
   - the five columns with the types and nullability from Data model
   - `product_pkey`, `ix_product_category` and a check constraint on `price >= 0`
10. **Migration history:** `echo "select version, success from flyway_schema_history" | <psql>` returns exactly one row, `1 | t`.
11. **Empty table:** `echo "select count(*) from product" | <psql>` returns `0`.
12. **Price constraint:** `echo "insert into product(name, price, category) values ('x', -1, 'c')" | <psql>` fails with `violates check constraint`.
13. **pgAdmin:** catalogdb → Schemas → public → Tables lists `product` and `flyway_schema_history`.

## References
- PROJECT.md §4.1 Catalog (the `product` table and Flyway migrations), §7 Aspire AppHost (references, waits and the `SPRING_DATASOURCE_*` env contract), §8 Java service conventions (layers, JPA, Flyway, `ddl-auto=validate`, Testcontainers).
- PLAN.md Phase 3 → Catalog, spec 1 (`01-product-schema.md`). It replaces the old PLAN 2.2–2.4 wiring for catalog.
