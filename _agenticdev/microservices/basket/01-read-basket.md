# basket 01 — read basket

**Status:** Done (00595ad)

## Goal
Give Basket its Redis storage and the read endpoint: a buyer's basket is stored under `basket:{buyerId}` and returned with a computed total.
The Redis wiring and the read endpoint are kept in one spec because the wiring alone can't be verified. Spring ignores `SPRING_DATA_REDIS_*` without the Redis starter, and the starter's health check fails under Aspire without those variables.

## Context
- This is the first Basket spec. Today `basket` is the only entry in the leftover `for (const name of ['basket'])` loop in `src/apphost.mts` (endpoint 8080, `/health`, OTLP, cert trust, commented "Not wired to its data store yet (basket/01)"). It has no references.
- `redis` already exists (`await builder.addRedis('redis')`, from PLAN 2.1), but its result is not kept in a variable and nothing references it. `Aspire.Hosting.Redis` is already in `aspire.config.json`.
- Aspire 13's Redis is password-protected **and TLS-enabled** by default. Inspecting the running container (`docker inspect`) shows `redis-server --requirepass $REDIS_PASSWORD --tls-port 6379 --port 6380 ...` with Aspire's dev cert: TLS on 6379, plaintext only on 6380. The JVM doesn't trust that cert, so this spec turns TLS off with `redis.withoutHttpsCertificate()` (decided), and Redis then serves plaintext on 6379. Only basket uses Redis, and this is a local demo.
- Aspire's Redis password: `RedisResource` in `src/.aspire/modules/aspire.mts` exposes `host()`, `port()` (endpoint reference expressions) and `passwordParameter`. So besides the `SPRING_DATA_REDIS_HOST` / `SPRING_DATA_REDIS_PORT` contract in PROJECT §7, basket also needs `SPRING_DATA_REDIS_PASSWORD` (decided for this spec). basket runs as a container, so host and port must be the values Aspire resolves for the container network, not `localhost`.
- catalog's and ordering's registrations in `src/apphost.mts` are the pattern for a service registered on its own with `withReference` + `waitFor` + `withEnvironment`.
- The basket service has only the skeleton: `RootController` (`GET /` → `Basket API is running.`), `BasketApplication`, empty `client`, `domain` and `repository` packages (`package-info.java`), and `BasketApplicationTests` (root placeholder + `/health` UP). Its `pom.xml` has only `spring-boot-starter-webmvc`, `-actuator` and `-webmvc-test`.
- Once `spring-boot-starter-data-redis` is on the classpath, actuator adds a Redis health indicator. `/health` then reports DOWN without a reachable Redis, which is why `BasketApplicationTests` must import the Testcontainers config too.
- Spring Boot 4 ships Jackson 3 (`tools.jackson.databind.json.JsonMapper`, auto-configured as a bean). Use it, not `com.fasterxml.jackson.databind.ObjectMapper`.
- catalog's `TestcontainersConfiguration` and the test dependencies in `src/java-back/catalog/pom.xml` are the working Boot 4 reference for the Testcontainers setup.

## Depends on
Nothing.

## Scope
- [ ] AppHost: `redis.withoutHttpsCertificate()` (plaintext Redis); take `basket` out of the shared loop; reference + waitFor `redis`; set `SPRING_DATA_REDIS_HOST`, `SPRING_DATA_REDIS_PORT` and `SPRING_DATA_REDIS_PASSWORD` from the redis resource.
- [ ] pom: `spring-boot-starter-data-redis`, Testcontainers (core + JUnit Jupiter, via `spring-boot-testcontainers`).
- [ ] Basket storage: key `basket:{buyerId}` holding JSON `{ buyerId, items: [{ productId, productName, unitPrice, quantity }] }`, no TTL, read and written with `StringRedisTemplate` + Jackson.
- [ ] `GET /basket/{buyerId}` → `{ buyerId, items, total }`; a missing key returns an empty basket; `total` is computed, never stored; a `buyerId` that isn't a UUID → 400.
- [ ] Tests with Testcontainers Redis: missing key → empty basket; a stored basket → its lines and total; stored JSON shape and no TTL.

## Explicit non-goals
- No calls to Catalog or Ordering, no `client` code, and no `catalog.url` / `ordering.url` (basket/02 and basket/04).
- No add, update, delete or checkout endpoints (basket/02–04). `BasketRepository.save` exists only because tests (and later specs) need to write a basket.
- No TTL, no JPA or JDBC, no `@RedisHash` / Spring Data Redis repositories.
- No changes to catalog, ordering or postgres. The only change to the redis resource is `withoutHttpsCertificate()` (no `withPassword`, no data volume, no Spring SSL bundle).
- No Spring Security, and no validation of item contents beyond what Jackson enforces when reading.

## Data model
**Redis:** one string key per buyer.

| Key | Value | TTL |
|---|---|---|
| `basket:{buyerId}` (lowercase UUID) | JSON string `{"buyerId":"<uuid>","items":[{"productId":1,"productName":"Mug","unitPrice":9.90,"quantity":2}]}` | none |

The stored JSON never contains `total`.

- **Domain** (`domain` package, Java records, no Lombok):
  - `BasketItem(long productId, String productName, BigDecimal unitPrice, int quantity)`.
  - `Basket(UUID buyerId, List<BasketItem> items)`. The compact constructor copies `items` with `List.copyOf` (null → empty list). `static Basket empty(UUID buyerId)`. `BigDecimal total()` returns `Σ unitPrice × quantity` with scale 2 (`0.00` for an empty basket). `total()` is not a JavaBean getter, so Jackson does not serialize it into Redis.
- **Repository** `repository.BasketRepository` (`@Repository`, constructor-injected `StringRedisTemplate` and `JsonMapper`):
  - `static String key(UUID buyerId)` → `"basket:" + buyerId`.
  - `Optional<Basket> find(UUID buyerId)`: `GET` the key; empty if missing, otherwise deserialize.
  - `void save(Basket basket)`: serialize and `SET` the key with no expiry.
- **API DTOs** (`api` package, records):
  - `BasketItemDto(long productId, String productName, BigDecimal unitPrice, int quantity)`.
  - `BasketDto(UUID buyerId, List<BasketItemDto> items, BigDecimal total)`, with `static BasketDto from(Basket)`.
- **Controller** `api.BasketController`: `GET /basket/{buyerId}` with `@PathVariable UUID buyerId` → `200 BasketDto`. Missing key → `Basket.empty(buyerId)`. A non-UUID path segment fails type conversion and Spring returns 400.
- **How the data store is created:** nothing to create. Redis is schemaless; the key appears on first `save`.

## Tasks
1. **AppHost** (`src/apphost.mts`):
   - Keep the result of `builder.addRedis('redis').withoutHttpsCertificate()` in a `redis` variable. Update the data-infrastructure comment to say Redis runs without TLS.
   - Remove the `for (const name of ['basket'])` loop and its comment. Register `basket` on its own with `addDockerfile('basket', './java-back/basket')`, `withHttpEndpoint({ targetPort: 8080 })`, `withHttpHealthCheck({ path: '/health' })`, `withOtlpExporter()` and `withCertificateTrustEnvironment('OTEL_EXPORTER_OTLP_CERTIFICATE')`.
   - Add `withReference(redis)` and `waitFor(redis)`.
   - Add `withEnvironment` for:
     - `SPRING_DATA_REDIS_HOST` = `await redis.host()`
     - `SPRING_DATA_REDIS_PORT` = `await redis.port()`
     - `SPRING_DATA_REDIS_PASSWORD` = `redis.passwordParameter.get()`
   - Comment above it: "Basket owns baskets in Redis. Spring needs host/port/password, not Aspire's connection string."
   - Every builder call is awaited (`no-floating-promises`).
2. **pom** (`src/java-back/basket/pom.xml`), no hard-coded versions:
   - Main: `spring-boot-starter-data-redis`.
   - Test: `spring-boot-testcontainers` and `org.testcontainers:testcontainers-junit-jupiter` (brings the core `GenericContainer`).
3. **`application.yml`**: no Redis values (they come from the environment or `@ServiceConnection`). Leave the rest unchanged.
4. **Code**: `domain/Basket.java`, `domain/BasketItem.java`, `repository/BasketRepository.java`, `api/BasketDto.java`, `api/BasketItemDto.java`, `api/BasketController.java` as in Data model. Delete `package-info.java` in `domain` and `repository`; keep the one in `client` (basket/02). Keep `RootController`.
5. **Tests**:
   - `TestcontainersConfiguration` (`@TestConfiguration(proxyBeanMethods = false)`) with a `@Bean @ServiceConnection(name = "redis") GenericContainer<?>` on `redis:8.10`, `withExposedPorts(6379)`. This is the same image the Aspire Redis resource runs (checked on 2026-11-24), so tests and `aspire run` use the same Redis version.
   - `BasketApplicationTests`: add `@Import(TestcontainersConfiguration.class)`; both existing tests still pass (`/health` is UP because Redis is reachable).
   - `repository/BasketRepositoryTests` (`@SpringBootTest` + `@Import`; `@BeforeEach` deletes every `basket:*` key):
     - `findMissingReturnsEmpty`: `find(randomUUID)` is empty.
     - `saveThenFindRoundTrips`: save a basket with `(1, "Mug", 9.90, 2)` and `(2, "Tee", 19.50, 1)`; `find` returns the same buyerId and both lines (prices compared with `isEqualByComparingTo`).
     - `storesJsonWithoutTotalOrTtl`: after `save`, the raw `StringRedisTemplate` value at `basket:{buyerId}` parses to an object with exactly the fields `buyerId` and `items` (no `total`), and `getExpire(key)` is `-1`.
   - `api/BasketControllerTests` (`@SpringBootTest` + `@AutoConfigureMockMvc` + `@Import`; same cleanup):
     - `missingBasketReturnsEmpty`: `GET /basket/{uuid}` → 200, `$.buyerId` = the uuid, `$.items` empty, `$.total` = 0.
     - `storedBasketReturnsLinesAndTotal`: write the raw JSON from Data model (Mug 9.90×2, Tee 19.50×1) with `StringRedisTemplate`; `GET` → 200, 2 items in stored order with their fields, `$.total` = 39.30.
     - `invalidBuyerIdReturns400`: `GET /basket/not-a-uuid` → 400.
   - Unit `domain/BasketTests` (no Spring): `emptyBasketTotalIsZero` (`0.00`), `totalIsSumOfLines` (`39.30`).

## Files touched
- `src/apphost.mts` (edit)
- `src/java-back/basket/pom.xml` (edit)
- `src/java-back/basket/src/main/java/br/com/guilherme/eshop/basket/domain/Basket.java` (new)
- `src/java-back/basket/src/main/java/br/com/guilherme/eshop/basket/domain/BasketItem.java` (new)
- `src/java-back/basket/src/main/java/br/com/guilherme/eshop/basket/domain/package-info.java` (delete)
- `src/java-back/basket/src/main/java/br/com/guilherme/eshop/basket/repository/BasketRepository.java` (new)
- `src/java-back/basket/src/main/java/br/com/guilherme/eshop/basket/repository/package-info.java` (delete)
- `src/java-back/basket/src/main/java/br/com/guilherme/eshop/basket/api/BasketDto.java` (new)
- `src/java-back/basket/src/main/java/br/com/guilherme/eshop/basket/api/BasketItemDto.java` (new)
- `src/java-back/basket/src/main/java/br/com/guilherme/eshop/basket/api/BasketController.java` (new)
- `src/java-back/basket/src/test/java/br/com/guilherme/eshop/basket/TestcontainersConfiguration.java` (new)
- `src/java-back/basket/src/test/java/br/com/guilherme/eshop/basket/BasketApplicationTests.java` (edit)
- `src/java-back/basket/src/test/java/br/com/guilherme/eshop/basket/domain/BasketTests.java` (new)
- `src/java-back/basket/src/test/java/br/com/guilherme/eshop/basket/repository/BasketRepositoryTests.java` (new)
- `src/java-back/basket/src/test/java/br/com/guilherme/eshop/basket/api/BasketControllerTests.java` (new)

`application.yml` is not expected to change. Nothing in catalog, ordering or the storefront changes.

## Definition of Done
Run every check from the repository root unless it says otherwise. `<basket-port>` is basket's HTTP port from the dashboard, and `<redis-container>` is the redis container name from `docker ps`. `<redis-cli>` is `docker exec -i <redis-container> sh -c 'redis-cli -a "$REDIS_PASSWORD" --no-auth-warning'` followed by the command. The redis container has `REDIS_PASSWORD` in its environment (verified on 2026-11-24).

0. **Redis without TLS:** `docker inspect <redis-container> --format '{{json .Config.Cmd}}'` shows `--requirepass` and no `--tls-port`.
1. **AppHost build:** in `src/`, `npm run build` exits 0 with no eslint or tsc errors.
2. **Tests:** with Docker running, `mvn -f src/java-back/basket/pom.xml test` prints `BUILD SUCCESS`, and the report lists `BasketApplicationTests` (2), `BasketTests` (2), `BasketRepositoryTests` (3) and `BasketControllerTests` (3), with 0 failures and 0 errors.
3. **Image:** `docker build src/java-back/basket` exits 0.
4. **Healthy start:** in `src/`, `aspire run` shows every resource (postgres, catalogdb, orderingdb, pgAdmin, redis, catalog, basket, ordering) as Running/Healthy. On a fresh start, basket shows *Waiting* until redis is healthy.
5. **basket env:** dashboard → basket → Environment shows `SPRING_DATA_REDIS_HOST` (not `localhost`), `SPRING_DATA_REDIS_PORT` (a number) and `SPRING_DATA_REDIS_PASSWORD` (set, not empty), and no `SPRING_DATASOURCE_*`.
6. **No leaks:** dashboard → catalog and ordering → Environment show no `SPRING_DATA_REDIS_*` and no `ConnectionStrings__redis`.
7. **Health:** `curl http://localhost:<basket-port>/health` returns `{"status":"UP"}`.
8. **Empty basket:** `curl -i http://localhost:<basket-port>/basket/3f1c2a9e-8b7d-4c6e-9a1b-2d3e4f5a6b7c` returns `200` and the body `{"buyerId":"3f1c2a9e-8b7d-4c6e-9a1b-2d3e4f5a6b7c","items":[],"total":0.00}` (the `total` may print as `0` or `0.00`).
9. **Read does not write:** `<redis-cli> EXISTS basket:3f1c2a9e-8b7d-4c6e-9a1b-2d3e4f5a6b7c` returns `0`.
10. **Stored basket:** put this JSON in a file `basket.json`: `{"buyerId":"11111111-1111-4111-8111-111111111111","items":[{"productId":1,"productName":"Mug","unitPrice":9.90,"quantity":2},{"productId":2,"productName":"Tee","unitPrice":19.50,"quantity":1}]}`, then run `docker exec -i <redis-container> sh -c 'redis-cli -a "$REDIS_PASSWORD" --no-auth-warning -x SET basket:11111111-1111-4111-8111-111111111111' < basket.json` → `OK`. `curl http://localhost:<basket-port>/basket/11111111-1111-4111-8111-111111111111` returns 200 with both items and `"total":39.30`.
11. **No TTL:** `<redis-cli> TTL basket:11111111-1111-4111-8111-111111111111` returns `-1`. Then clean up with `<redis-cli> DEL basket:11111111-1111-4111-8111-111111111111` → `1`.
12. **Bad id:** `curl -i http://localhost:<basket-port>/basket/not-a-uuid` returns `400`.
13. **Trace:** dashboard → Traces shows a `GET /basket/{buyerId}` span from basket with a child Redis `GET` span.

## References
- PROJECT.md §3 (buyerId is a UUID v4), §4.2 Basket (Redis key and JSON, `GET /basket/{buyerId}`, computed total, empty basket on a missing key), §7 Aspire AppHost (basket → redis, `SPRING_DATA_REDIS_HOST/PORT` env contract, only Basket references Redis), §8 Java service conventions (layers, Spring Data Redis only, Testcontainers Redis), §9 (no Basket TTL).
- PLAN.md Phase 3 → Basket, spec 1 (`01-read-basket.md`). It replaces the old PLAN 2.2 wiring for basket and part of 3.4.
- Pattern: `microservices/ordering/01-order-schema.md` (Done) for registering a service on its own in the AppHost with its data store.
