# catalog 01 — product schema

**Status:** Not started

## Goal
Give Catalog its own database: wire `catalog` to `catalogdb` in the AppHost and create the empty `product` table through Flyway, with the `Product` entity and repository that later specs build on.
The wiring and the schema are kept in one spec because neither can be verified alone. Spring ignores `SPRING_DATASOURCE_*` until JPA and the driver are on the classpath, so a wrong JDBC URL would go unnoticed. And JPA cannot start under Aspire without those variables.

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

## Definition of Done
- AppHost build passes.
- `mvn test` passes in catalog (Docker running).
- `aspire run`: every resource healthy, catalog waits for catalogdb.
- Dashboard → catalog env shows the three `SPRING_DATASOURCE_*` vars with a valid JDBC URL; basket and ordering have none.
- pgAdmin → catalogdb shows an empty `product` table and `flyway_schema_history` with V1.

## References
