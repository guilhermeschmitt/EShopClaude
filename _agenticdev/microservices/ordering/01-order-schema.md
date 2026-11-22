# ordering 01 — order schema

**Status:** Not started

## Goal
Give Ordering its own database: wire `ordering` to `orderingdb` in the AppHost and create the `orders` and `order_item` tables through Flyway, with the entities and repository later specs build on.
The wiring and the schema are kept in one spec because neither can be verified alone. Spring ignores `SPRING_DATASOURCE_*` until JPA and the driver are on the classpath, and JPA cannot start under Aspire without those variables.

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
- No endpoints.
- No order status, buyer name, email or address.
- No changes to catalog or basket registrations.

## Definition of Done
- AppHost build passes.
- `mvn test` passes in ordering.
- `aspire run`: every resource healthy, ordering waits for orderingdb.
- Dashboard → ordering env shows the three `SPRING_DATASOURCE_*` vars with a valid JDBC URL.
- pgAdmin → orderingdb shows `orders` and `order_item`.

## References
