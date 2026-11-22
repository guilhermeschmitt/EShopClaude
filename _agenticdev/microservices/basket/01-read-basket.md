# basket 01 — read basket

**Status:** Not started

## Goal
Give Basket its Redis storage and the read endpoint: a buyer's basket is stored under `basket:{buyerId}` and returned with a computed total.
The Redis wiring and the read endpoint are kept in one spec because the wiring alone can't be verified. Spring ignores `SPRING_DATA_REDIS_*` without the Redis starter, and the starter's health check fails under Aspire without those variables.

## Depends on
Nothing.

## Scope
- [ ] AppHost: take `basket` out of the shared loop; reference + waitFor `redis`; set `SPRING_DATA_REDIS_HOST` / `SPRING_DATA_REDIS_PORT`.
- [ ] pom: `spring-boot-starter-data-redis`, Testcontainers.
- [ ] Basket storage: key `basket:{buyerId}` holding JSON `{ buyerId, items: [{ productId, productName, unitPrice, quantity }] }`, no TTL.
- [ ] `GET /basket/{buyerId}` → `{ buyerId, items, total }`; a missing key returns an empty basket; `total` is computed, never stored.
- [ ] Tests with Testcontainers Redis: missing key → empty basket; a stored basket → its lines and total.

## Explicit non-goals
- No calls to Catalog or Ordering, and no `catalog.url` / `ordering.url`.
- No add, update or checkout endpoints.
- No TTL, no JPA or JDBC.

## Definition of Done
- AppHost build passes.
- `mvn test` passes in basket.
- `aspire run`: every resource healthy, basket waits for redis.
- Dashboard → basket env shows the two Redis vars; catalog and ordering have none.
- curl `GET /basket/<new uuid>` against the basket endpoint URL returns an empty basket with total 0.

## References
