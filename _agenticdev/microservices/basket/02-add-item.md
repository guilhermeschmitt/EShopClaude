# basket 02 — add item

**Status:** Done (64a461a)

## Goal
Add a product to the basket. Basket looks the product up in Catalog, rejects unknown ids and stores a snapshot of the name and price. This is the first service-to-service call, so the Basket → Catalog wiring comes with it.

## Context
- basket/01 is merged (00595ad). In `src/apphost.mts`, `basket` is registered on its own: `addDockerfile('basket', './java-back/basket')`, endpoint 8080, `/health`, OTLP, cert trust, `withReference(redis)` + `waitFor(redis)` and `SPRING_DATA_REDIS_HOST/PORT/PASSWORD`. It has no reference to `catalog`.
- `catalog` is registered with `await builder.addDockerfile('catalog', ...)...`, but the result isn't kept in a variable, so nothing can reference it yet.
- **`withReference(catalog)` doesn't work here.** The TypeScript signature accepts a `ContainerResource`, but at runtime the AppHost fails with *"The resource 'catalog' can't be used with withReference because it doesn't provide a connection string, service discovery, or a custom withReference implementation"*. This was found while verifying this spec on 2026-11-24. Instead, basket gets `withEnvironment('services__catalog__http__0', catalog.getEndpoint('http'))`, which takes an `EndpointReference`, plus `waitFor(catalog)`. The variable name stays the same, so `application.yml` and PROJECT §7's env contract are unchanged.
- catalog/04 is merged: `GET /products/{id}` returns `ProductDto { id, name, description, price, category }`, or 404 (a `ResponseStatusException`). The seed (`V2__seed_products.sql`, inserted in order) makes id 1 `Classic Logo T-Shirt` at 19.90 and id 2 `Minimalist Black T-Shirt` at 22.50.
- Basket code today:
  - `domain.Basket(UUID buyerId, List<BasketItem> items)` with `empty()` and a computed `total()`;
  - `domain.BasketItem(long productId, String productName, BigDecimal unitPrice, int quantity)`;
  - `repository.BasketRepository` with `find(UUID)` and `save(Basket)`;
  - `api.BasketController` with only `GET /basket/{buyerId}`;
  - `api.BasketDto` / `BasketItemDto`;
  - an empty `client` package (`package-info.java`).
- Tests today: `BasketApplicationTests` (2), `BasketTests` (2), `BasketRepositoryTests` (3), `BasketControllerTests` (3), with `TestcontainersConfiguration` (`redis:8.10`, `@ServiceConnection`). There is no `src/test/resources`.
- `application.yml` has `server.port`, `spring.application.name` and the actuator `/health` settings. There is no `catalog.url` yet.
- Spring Boot 4 split out the HTTP client auto-configuration. The auto-configured `RestClient.Builder` comes from `spring-boot-starter-restclient`, and `@AutoConfigureMockRestServiceServer` comes from `spring-boot-starter-restclient-test` (package `org.springframework.boot.restclient.test.autoconfigure`). Neither is on basket's classpath yet. The plan must confirm that both artifacts resolve under `spring-boot-starter-parent` 4.2.0 before writing code.
- The OpenTelemetry Java agent already instruments the outgoing HTTP call, so the basket → catalog trace needs no code.

## Depends on
- `basket/01-read-basket`
- `catalog/04-get-product`

## Scope
- [ ] AppHost: keep `catalog` in a variable. basket gets `withEnvironment('services__catalog__http__0', catalog.getEndpoint('http'))` and `waitFor(catalog)`, not `withReference` (see Context).
- [ ] `application.yml`: `catalog.url: ${services__catalog__http__0}` (no default, so a missing env var fails at startup).
- [ ] `client/CatalogClient` (Spring `RestClient` built from Boot's `RestClient.Builder`, base URL `catalog.url`) calls `GET /products/{id}` and returns `Optional<CatalogProduct>`: empty on 404, and any other error propagates.
- [ ] `POST /basket/{buyerId}/items` `{ productId, quantity }`:
  - Validation runs before any Catalog call. A non-UUID `buyerId` → 400, a missing `productId` → 400, and `quantity < 1` → 400. An omitted `quantity` means 1.
  - Catalog 404 → 404, and Redis is not touched.
  - A new product appends a line with Catalog's name and price.
  - An existing product adds `quantity` to the line **and refreshes** its `productName`/`unitPrice` from Catalog (decided). The line keeps its position.
  - Saves, then returns **200** with the basket (same shape as `GET`).
  - Any other Catalog failure propagates as a 500 (decided, PROJECT §2 Resilience).
- [ ] Tests with `MockRestServiceServer` and Testcontainers Redis.

## Explicit non-goals
- No quantity update or remove (basket/03), and no checkout, no `ordering.url` and no Ordering client (basket/04).
- Reading the basket (`GET`) still never calls Catalog.
- No retries, fallbacks, timeouts tuning or circuit breakers, and no 502 mapping (decided: errors propagate as 500).
- No Bean Validation dependency (decided: manual checks), and no upper limit on quantity.
- No locking or optimistic concurrency on the Redis read-modify-write. Concurrent adds for one buyer can lose an update, which is accepted for the demo.
- No changes to catalog, ordering, redis, postgres or the storefront.

## Data model
- **Redis:** unchanged from basket/01. The key is `basket:{buyerId}`, the JSON is `{ buyerId, items: [{ productId, productName, unitPrice, quantity }] }`, and there is no TTL.
- **Domain** `Basket`: add `Basket withItem(long productId, String productName, BigDecimal unitPrice, int quantity)`, which returns a new `Basket`.
  - If a line with `productId` exists, it is replaced in the same position by `(productId, productName, unitPrice, existing.quantity + quantity)`.
  - Otherwise the new line is appended.
- **Client** (`client` package):
  - `CatalogProduct(long id, String name, BigDecimal price)` is a record. Unknown JSON fields (`description`, `category`) are ignored.
  - `CatalogClient` is a `@Component` whose constructor takes `RestClient.Builder` and `@Value("${catalog.url}") String catalogUrl`. Its `RestClient` is built with `baseUrl(catalogUrl)`.
  - `Optional<CatalogProduct> findProduct(long id)` calls `GET /products/{id}`. It catches `HttpClientErrorException.NotFound` and returns `Optional.empty()`, and lets everything else propagate.
- **API** (`api` package):
  - `AddItemRequest(Long productId, Integer quantity)` is a record with boxed fields, so the controller can detect values that are missing.
  - `BasketController` now also takes `CatalogClient`. It adds `@PostMapping("/{buyerId}/items")` with `@PathVariable UUID buyerId` and `@RequestBody AddItemRequest`, and returns `BasketDto`.
  - Validation failures throw `ResponseStatusException(BAD_REQUEST, …)`. An unknown product throws `ResponseStatusException(NOT_FOUND, "product <id> not found")`.
- **How the data store is created:** nothing to create (Redis is schemaless). Catalog is reached through `catalog.url`, which Aspire injects as `services__catalog__http__0`.

## Tasks
1. **AppHost** (`src/apphost.mts`):
   - `const catalog = await builder.addDockerfile('catalog', …)…`, with the chain unchanged.
   - On basket, add `.withEnvironment('services__catalog__http__0', catalog.getEndpoint('http'))` and `.waitFor(catalog)`, with a comment explaining why it isn't `withReference`.
   - Update basket's comment to: "Basket owns baskets in Redis and looks products up in Catalog. Spring needs host/port/password, not Aspire's connection string."
   - Every builder call is awaited.
2. **pom** (`src/java-back/basket/pom.xml`), with no hard-coded versions:
   - Main: `spring-boot-starter-restclient`.
   - Test: `spring-boot-starter-restclient-test`.
3. **`src/main/resources/application.yml`:** add the top-level `catalog.url: ${services__catalog__http__0}` with a comment that the AppHost injects it from catalog's endpoint.
4. **`src/test/resources/config/application.yml`** (new): `catalog.url: http://catalog.test`. Boot loads `classpath:/config/application.yml` on top of `classpath:/application.yml`, so the existing test classes need no change.
5. **Domain:** add `Basket.withItem(...)` as in Data model.
6. **Client:** `client/CatalogProduct.java` and `client/CatalogClient.java`. Delete `client/package-info.java`.
7. **API:** `api/AddItemRequest.java`, plus the `POST` handler in `api/BasketController.java`. The order is: validate → `catalog.findProduct` (404 if empty) → `baskets.find(buyerId).orElseGet(empty)` → `withItem(...)` → `baskets.save` → `BasketDto.from`.
8. **Tests:**
   - `domain/BasketTests` gets two new tests:
     - `withItemAppendsNewLine`: an empty basket plus `(1, "Mug", 9.90, 2)` gives one line and a total of `19.80`.
     - `withItemIncrementsAndRefreshesExistingLine`: a basket `[(1,"Old",5.00,1),(2,"Tee",19.50,1)]` plus `(1,"Mug",9.90,2)` gives line 0 = `(1,"Mug",9.90,3)`, line 1 unchanged and a total of `49.20`.
   - `api/AddItemTests` (new) uses `@SpringBootTest`, `@AutoConfigureMockMvc`, `@AutoConfigureMockRestServiceServer` and `@Import(TestcontainersConfiguration.class)`. It injects `MockRestServiceServer`, `MockMvc` and `StringRedisTemplate`, and `@BeforeEach` deletes every `basket:*` key. Catalog is stubbed at `http://catalog.test/products/{id}` with `ProductDto` JSON including `description` and `category`.
     - `addNewProductSnapshotsCatalogData`: Catalog returns id 1 `Classic Logo T-Shirt` at 19.90. `POST {"productId":1}` → 200, one item `(1, "Classic Logo T-Shirt", 19.90, 1)` and `$.total` 19.90. The Redis key exists, and `server.verify()` passes.
     - `addExistingProductIncrementsAndRefreshesSnapshot`: Redis holds a stored line `(1, "Old name", 5.00, 1)`. Catalog returns 19.90 `Classic Logo T-Shirt`. `POST {"productId":1,"quantity":2}` → 200, item `(1, "Classic Logo T-Shirt", 19.90, 3)` and `$.total` 59.70.
     - `unknownProductReturns404`: Catalog returns 404. `POST {"productId":99999}` → 404, and the Redis key does not exist.
     - `quantityBelowOneReturns400`: `POST {"productId":1,"quantity":0}` → 400. No Catalog request is made (`server.verify()` with no expectations).
     - `missingProductIdReturns400`: `POST {"quantity":1}` → 400. No Catalog request is made.
     - `getDoesNotCallCatalog`: stores a basket, then `GET /basket/{id}` → 200, and `server.verify()` with no expectations passes.
   - Existing tests are unchanged and still pass.

## Files touched
- `src/apphost.mts` (edit)
- `src/java-back/basket/pom.xml` (edit)
- `src/java-back/basket/src/main/resources/application.yml` (edit)
- `src/java-back/basket/src/main/java/br/com/guilherme/eshop/basket/domain/Basket.java` (edit)
- `src/java-back/basket/src/main/java/br/com/guilherme/eshop/basket/client/package-info.java` (delete)
- `src/java-back/basket/src/main/java/br/com/guilherme/eshop/basket/client/CatalogProduct.java` (new)
- `src/java-back/basket/src/main/java/br/com/guilherme/eshop/basket/client/CatalogClient.java` (new)
- `src/java-back/basket/src/main/java/br/com/guilherme/eshop/basket/api/AddItemRequest.java` (new)
- `src/java-back/basket/src/main/java/br/com/guilherme/eshop/basket/api/BasketController.java` (edit)
- `src/java-back/basket/src/test/resources/config/application.yml` (new)
- `src/java-back/basket/src/test/java/br/com/guilherme/eshop/basket/domain/BasketTests.java` (edit)
- `src/java-back/basket/src/test/java/br/com/guilherme/eshop/basket/api/AddItemTests.java` (new)

Nothing in catalog, ordering or the storefront changes.

## Definition of Done
Run every check from the repository root in Git Bash unless it says otherwise.
- `<basket-port>` and `<catalog-port>` are the HTTP ports shown in the dashboard.
- `<redis-cli>` is `docker exec -i <redis-container> sh -c 'redis-cli -a "$REDIS_PASSWORD" --no-auth-warning'`, followed by the command, as in basket/01.
- `B` is `22222222-2222-4222-8222-222222222222`.

1. **AppHost build:** in `src/`, `npm run build` exits 0 with no eslint or tsc errors.
2. **Tests:** with Docker running, `mvn -f src/java-back/basket/pom.xml test` prints `BUILD SUCCESS`. The report lists these classes, with 0 failures and 0 errors:
   - `BasketApplicationTests` (2)
   - `BasketTests` (4)
   - `BasketRepositoryTests` (3)
   - `BasketControllerTests` (3)
   - `AddItemTests` (6)
3. **Image:** `docker build src/java-back/basket` exits 0.
4. **Healthy start:** in `src/`, `aspire run` shows every resource (postgres, catalogdb, orderingdb, pgAdmin, redis, catalog, basket, ordering) as Running/Healthy. On a fresh start, basket shows *Waiting* until both redis and catalog are healthy.
5. **basket env:** dashboard → basket → Environment shows `services__catalog__http__0` set to a URL that is not `localhost`. catalog and ordering → Environment show no `services__basket__*`.
6. **Seed check:**
   - `curl http://localhost:<catalog-port>/products/1` → 200, `"name":"Classic Logo T-Shirt"`, `"price":19.90`.
   - `/products/2` → 200, `"name":"Minimalist Black T-Shirt"`, `"price":22.50`.
7. **Add new:** `curl -i -X POST -H 'Content-Type: application/json' -d '{"productId":1}' http://localhost:<basket-port>/basket/$B/items` → `200`. The body has one item `{"productId":1,"productName":"Classic Logo T-Shirt","unitPrice":19.90,"quantity":1}` and `"total":19.90`.
8. **Increment:** the same request again → 200, item 1 has `"quantity":2`, `"total":39.80`.
9. **Second product:** `-d '{"productId":2,"quantity":1}'` → 200. There are two items in order (1, then 2), and `"total":62.30`.
10. **Read back:** `curl http://localhost:<basket-port>/basket/$B` → 200 with the same two lines and `"total":62.30`.
11. **Unknown product:** `-d '{"productId":99999}'` → `404`. Then `curl .../basket/$B` still shows `"total":62.30`.
12. **Bad input:** `-d '{"productId":1,"quantity":0}'` → `400`; `-d '{}'` → `400`; `POST .../basket/not-a-uuid/items` with `{"productId":1}` → `400`.
13. **Stored shape:** `<redis-cli> GET basket:$B` returns JSON with only `buyerId` and `items` (no `total`), and `<redis-cli> TTL basket:$B` returns `-1`. Clean up with `<redis-cli> DEL basket:$B` → `1`.
14. **Trace:** dashboard → Traces shows one trace for `POST /basket/{buyerId}/items` that contains, in order: a basket server span → a basket HTTP client `GET` span → a catalog `GET /products/{id}` server span → a Postgres `SELECT` span on `catalogdb`, plus basket's Redis `GET`/`SET` spans.

## References
- PROJECT.md:
  - §2 Communication (synchronous HTTP through service discovery) and Resilience (errors propagate, no retries);
  - §4.1 (`GET /products/{id}`, `Product` shape);
  - §4.2 Basket (client sends only `productId` and `quantity`; Basket looks the product up on add, 404 for unknown, stores a snapshot; reading never calls Catalog; `POST /basket/{buyerId}/items` adds or increments and returns the basket);
  - §7 (basket → catalog reference and wait; `services__catalog__http__0` → `catalog.url`);
  - §8 (`client` layer with `RestClient`; Basket tests stub Catalog with `MockRestServiceServer`);
  - §9 (no retries or circuit breakers, no price-drift detection).
- PLAN.md Phase 3 → Basket, spec 2 (`02-add-item.md`). It replaces old PLAN 4.1 (Basket → Catalog communication) and the catalog part of PLAN 1.4's basket wiring.
- Pattern: `microservices/basket/01-read-basket.md` (Done) for the Redis repository, Testcontainers setup and DoD style.
