# basket 04 — checkout

**Status:** Done (5ee7850)

## Goal
Turn the basket into an order. Basket sends its lines with the snapshot prices to Ordering and clears the basket only after the order exists. The Basket → Ordering wiring comes with it.
The success path and the failure path stay in one spec: they are the same flow, and "the basket is kept when Ordering fails" can only be checked against a working checkout.

## Context
- basket/03 is merged (6fe653c), and ordering/02 (78c10d4) and ordering/04 (df164a2) are merged.
- `src/apphost.mts` today:
  - `basket` is registered with Redis (`SPRING_DATA_REDIS_HOST/PORT/PASSWORD`), `services__catalog__http__0` from `catalog.getEndpoint('http')`, `waitFor(redis)` and `waitFor(catalog)`.
  - `ordering` is registered as `await builder.addDockerfile('ordering', …)…` without being assigned to a variable, so basket can't reference it yet.
  - Dockerfile containers reject `withReference(<java service>)`. A reference to a Java service is `withEnvironment('services__<name>__http__0', <name>.getEndpoint('http'))` plus `waitFor(<name>)` (PROJECT §7).
- Basket code today:
  - `domain.Basket(UUID buyerId, List<BasketItem> items)` has `empty()`, `withItem(...)`, `withQuantity(...)` and a computed `total()` (scale 2).
  - `domain.BasketItem(long productId, String productName, BigDecimal unitPrice, int quantity)`.
  - `repository.BasketRepository` has `key(UUID)`, `find(UUID)` and `save(Basket)` (JSON under `basket:{buyerId}`, no TTL). There is no delete method.
  - `api.BasketController` has `GET /basket/{buyerId}`, `POST /basket/{buyerId}/items` and `PUT /basket/{buyerId}/items/{productId}`. Validation is done by hand with `ResponseStatusException`, and there is no Bean Validation or exception handler.
  - `client.CatalogClient` is a `@Component` that builds its `RestClient` from the injected `RestClient.Builder` with `baseUrl(${catalog.url})`. `client.CatalogProduct(long id, String name, BigDecimal price)`.
  - `application.yml` maps `catalog.url: ${services__catalog__http__0}` with no default. `src/test/resources/config/application.yml` sets `catalog.url: http://catalog.test`.
- Ordering's `POST /orders` takes `{ buyerId, items: [{ productId, productName, unitPrice, quantity }] }` and returns 201 with `{ id, buyerId, createdAt, total, items[] }`. It returns 400 on invalid input. `GET /orders/{id}` returns the order, or 404.
- Tests today, 27 in total:
  - `BasketApplicationTests` (2)
  - `BasketTests` (7)
  - `BasketRepositoryTests` (3)
  - `BasketControllerTests` (3)
  - `AddItemTests` (6)
  - `UpdateItemTests` (6)

  The controller tests use `@SpringBootTest`, `@AutoConfigureMockMvc`, `@AutoConfigureMockRestServiceServer` and `@Import(TestcontainersConfiguration.class)`, and they autowire a single `MockRestServiceServer`.
- **Constraint (decided):** Boot's `RestClient.Builder` bean is prototype-scoped. With two clients, the auto-configured `MockRestServiceServer` would be bound to two builders and fail with "bound to more than one RestClient". Both clients must therefore build from **one shared `RestClient.Builder` instance**, and each one `clone()`s it before setting its base URL. The one `MockRestServiceServer` then stubs both peers by their full URLs.

## Depends on
- `basket/03-update-item-quantity`
- `ordering/02-create-order`
- `ordering/04-get-order`

## Scope
- [ ] AppHost: assign `ordering` to a `const`. On `basket`, add `withEnvironment('services__ordering__http__0', ordering.getEndpoint('http'))` and `waitFor(ordering)`.
- [ ] `application.yml` maps `ordering.url: ${services__ordering__http__0}` with no default. The test config sets `ordering.url: http://ordering.test`.
- [ ] A shared `RestClient.Builder` (decided): one singleton builder for both peer clients, created through Boot's `RestClientBuilderConfigurer` so that customizers, including the test `MockServerRestClientCustomizer`, still apply. `CatalogClient` and `OrderingClient` each `clone()` it. The exact wiring is confirmed in plan mode.
- [ ] `client/OrderingClient` (`RestClient`, base URL `ordering.url`) with `createOrder(UUID buyerId, List<BasketItem> items)` → POST `/orders` → returns the created order's id. It translates nothing, so every failure propagates.
- [ ] `POST /basket/{buyerId}/checkout`:
  - A non-UUID `buyerId` → 400 (conversion).
  - A missing basket, or `items: []` → 400, and nothing is written and Ordering isn't called.
  - It POSTs `{ buyerId, items }` with the snapshot name, price and quantity of every line, in basket order. It never calls Catalog.
  - Only after Ordering returns successfully does it delete `basket:{buyerId}` and return **201** `{ "orderId": <id> }`.
- [ ] Failure (decided: plain 500): any exception from Ordering (a 4xx or 5xx response, or an unreachable host) propagates without a handler. Spring returns its default **500**, and the basket key is left exactly as it was.
- [ ] `BasketRepository.delete(UUID buyerId)`.
- [ ] Tests with Testcontainers Redis and `MockRestServiceServer` stubbing Ordering and proving Catalog is never called (see Tasks).

## Explicit non-goals
- No re-pricing and no Catalog call at checkout (PROJECT §4.2, §9).
- No retries, compensation, sagas, events, circuit breakers or `@ExceptionHandler`/502 mapping (PROJECT §2, §9).
- No protection against two concurrent checkouts of the same basket (no lock or `WATCH`). The storefront disables the button (PROJECT §5 step 1).
- No changes to Ordering, Catalog or the storefront, and no new Maven dependencies.
- No storefront `/checkout` page (PLAN 5.4).

## Data model
- **Redis:** the key and JSON are unchanged. A successful checkout deletes `basket:{buyerId}` (the key no longer exists, and `GET` returns the empty basket). A failed checkout leaves the key byte-for-byte unchanged.
- **Repository:** `BasketRepository.delete(UUID buyerId)` → `redis.delete(key(buyerId))`.
- **Client** (`client` package):
  - `CreateOrderRequest(UUID buyerId, List<CreateOrderItem> items)` and `CreateOrderItem(long productId, String productName, BigDecimal unitPrice, int quantity)`. These are records with Ordering's JSON field names, built from the `BasketItem`s.
  - `CreatedOrder(long id)` is the part of Ordering's `Order` response that Basket reads. Other fields are ignored.
  - `OrderingClient.createOrder(UUID buyerId, List<BasketItem> items)` returns `long` (the order id).
- **API** (`api` package): `CheckoutResponse(long orderId)`.
- **Configuration:** a new `ordering.url` property from the env var `services__ordering__http__0`.
- **How the data store is created:** nothing to create. Redis is schemaless. The only new env var is `services__ordering__http__0` on basket, which the AppHost sets.

## Tasks
1. **AppHost** (`src/apphost.mts`): `const ordering = await builder.addDockerfile('ordering', …)` (unchanged otherwise). On `basket`, after the catalog lines, add `.withEnvironment('services__ordering__http__0', ordering.getEndpoint('http'))` and `.waitFor(ordering)`. Update the basket comment so it mentions Ordering.
2. **Config:**
   - `src/main/resources/application.yml`: add `ordering.url: ${services__ordering__http__0}` with a comment matching the catalog one.
   - `src/test/resources/config/application.yml`: add `ordering.url: http://ordering.test`.
3. **Shared builder** (`client/RestClientConfig.java`, new): a `@Configuration` that exposes one singleton `RestClient.Builder` made with `RestClientBuilderConfigurer.configure(RestClient.builder())`.
   - `CatalogClient` changes to `builder.clone().baseUrl(catalogUrl).build()`.
   - If plan mode finds a simpler way that gives the same single binding, it uses that and records it in the plan.
4. **Client:** add `client/CreateOrderRequest.java`, `client/CreateOrderItem.java`, `client/CreatedOrder.java` and `client/OrderingClient.java`.
   - `OrderingClient` builds its client with `builder.clone().baseUrl(orderingUrl).build()`.
   - `createOrder` calls `rest.post().uri("/orders").contentType(APPLICATION_JSON).body(request).retrieve().body(CreatedOrder.class).id()`.
5. **Repository:** add `delete(UUID)` to `BasketRepository`.
6. **API:**
   - Add `api/CheckoutResponse.java`.
   - `BasketController` gets `OrderingClient` injected and `@PostMapping("/{buyerId}/checkout")` returning `ResponseEntity<CheckoutResponse>`.
   - The handler's order is: `baskets.find(buyerId)` → if missing or `items` is empty, `ResponseStatusException(BAD_REQUEST, "basket is empty")` → `ordering.createOrder(buyerId, basket.items())` → `baskets.delete(buyerId)` → `201` with `CheckoutResponse(orderId)`.
7. **Repository test** (`repository/BasketRepositoryTests.java`): add `deleteRemovesKey`. Save a basket, `delete` it, and then `find` is empty and `hasKey` is false.
8. **Controller tests:** a new `api/CheckoutTests.java`, set up like `UpdateItemTests`:
   - `@BeforeEach` resets the mock server and deletes every `basket:*` key. Stored basket = `[(1,"Mug",9.90,2),(2,"Tee",19.50,1)]`. Every test ends with `server.verify()`, and no expectation is ever set for `http://catalog.test/**`, which proves Catalog is never called.
   - `checkoutCreatesOrderAndClearsBasket`: expect `POST http://ordering.test/orders` with JSON body `{"buyerId":"<B>","items":[{"productId":1,"productName":"Mug","unitPrice":9.90,"quantity":2},{"productId":2,"productName":"Tee","unitPrice":19.50,"quantity":1}]}`, and respond 201 `{"id":42,…}`. The checkout returns 201 with `$.orderId` = 42. Afterwards the key doesn't exist and `GET` returns `items: []` and total 0.
   - `orderingServerErrorKeepsBasket`: Ordering responds 500. The checkout returns 500, and the stored JSON is unchanged.
   - `orderingRejectsKeepsBasket`: Ordering responds 400. The checkout returns 500, and the stored JSON is unchanged.
   - `orderingUnreachableKeepsBasket`: Ordering responds `withException(new java.net.ConnectException())`. The checkout returns 500, and the stored JSON is unchanged.
   - `emptyBasketReturns400`: the stored basket has `items: []`. The checkout returns 400, there is no Ordering call, and the key is still there.
   - `missingBasketReturns400`: a random buyer with no key gets 400, there is no Ordering call, and the key doesn't exist afterwards.
   - Note: the 500 tests rely on MockMvc rendering the unhandled exception as 500. If MockMvc rethrows instead, the test asserts the thrown `RestClientException` (wrapped in a `ServletException`) and the unchanged basket. The real HTTP 500 is checked in the DoD.
9. Existing tests are unchanged apart from the `CatalogClient` builder change, and they still pass.

## Files touched
- `src/apphost.mts` (edit)
- `src/java-back/basket/src/main/resources/application.yml` (edit)
- `src/java-back/basket/src/test/resources/config/application.yml` (edit)
- `src/java-back/basket/src/main/java/br/com/guilherme/eshop/basket/client/RestClientConfig.java` (new)
- `src/java-back/basket/src/main/java/br/com/guilherme/eshop/basket/client/CatalogClient.java` (edit)
- `src/java-back/basket/src/main/java/br/com/guilherme/eshop/basket/client/OrderingClient.java` (new)
- `src/java-back/basket/src/main/java/br/com/guilherme/eshop/basket/client/CreateOrderRequest.java` (new)
- `src/java-back/basket/src/main/java/br/com/guilherme/eshop/basket/client/CreateOrderItem.java` (new)
- `src/java-back/basket/src/main/java/br/com/guilherme/eshop/basket/client/CreatedOrder.java` (new)
- `src/java-back/basket/src/main/java/br/com/guilherme/eshop/basket/repository/BasketRepository.java` (edit)
- `src/java-back/basket/src/main/java/br/com/guilherme/eshop/basket/api/CheckoutResponse.java` (new)
- `src/java-back/basket/src/main/java/br/com/guilherme/eshop/basket/api/BasketController.java` (edit)
- `src/java-back/basket/src/test/java/br/com/guilherme/eshop/basket/repository/BasketRepositoryTests.java` (edit)
- `src/java-back/basket/src/test/java/br/com/guilherme/eshop/basket/api/CheckoutTests.java` (new)

Catalog, ordering and the storefront don't change.

## Definition of Done
Run every check from the repository root in Git Bash unless it says otherwise.
- `<basket-port>` and `<ordering-port>` are the services' HTTP ports from `aspire describe`.
- `<redis-cli>` is `docker exec -i <redis-container> sh -c 'redis-cli -a "$REDIS_PASSWORD" --no-auth-warning'`, followed by the command, as in basket/01.
- `<ordering-container>` is the ordering container's name from `docker ps`.
- `B` is `55555555-5555-4555-8555-555555555555`.
- `POST` is `curl -s -i -X POST -H 'Content-Type: application/json'`.

1. **AppHost build:** in `src/`, `npm run build` exits 0.
2. **Tests:** with Docker running, `mvn -f src/java-back/basket/pom.xml test` prints `BUILD SUCCESS` with 0 failures and 0 errors. The report lists:
   - `BasketApplicationTests` (2)
   - `BasketTests` (7)
   - `BasketRepositoryTests` (4)
   - `BasketControllerTests` (3)
   - `AddItemTests` (6)
   - `UpdateItemTests` (6)
   - `CheckoutTests` (6)
3. **Image:** `docker build src/java-back/basket` exits 0.
4. **Healthy start:** in `src/`, `aspire run` brings up postgres, catalogdb, orderingdb, pgAdmin, redis, catalog, basket and ordering. `aspire wait <resource>` exits 0 for each one, and `aspire describe` shows each as Running/Healthy. `basket`'s environment in `aspire describe` (or the dashboard) contains `services__ordering__http__0`.
5. **Empty checkout:** `POST http://localhost:<basket-port>/basket/$B/checkout` → `400`, and `<redis-cli> EXISTS basket:$B` returns `0`.
6. **Fill the basket:**
   - `POST -d '{"productId":1}' .../basket/$B/items` → 200.
   - `POST -d '{"productId":1}' .../basket/$B/items` → 200.
   - `POST -d '{"productId":2}' .../basket/$B/items` → 200, with two items (product 1 quantity 2 at 19.90, product 2 quantity 1 at 22.50) and `"total":62.30`.
7. **Failure keeps the basket:**
   - Save `<redis-cli> GET basket:$B` as `before`.
   - `docker stop <ordering-container>`.
   - `POST .../basket/$B/checkout` → `HTTP/1.1 500`.
   - `<redis-cli> GET basket:$B` equals `before`, and `curl -s .../basket/$B` still shows `"total":62.30`.
   - `docker start <ordering-container>`, then `aspire wait ordering` exits 0.
8. **Checkout:** `POST .../basket/$B/checkout` → `HTTP/1.1 201` with the body `{"orderId":<N>}`.
9. **Basket cleared:** `<redis-cli> EXISTS basket:$B` returns `0`, and `curl -s .../basket/$B` returns `"items":[]` with `"total":0` (rendered as `0` or `0.00`).
10. **Order matches the snapshot:** `curl -s http://localhost:<ordering-port>/orders/<N>` → 200 with `"buyerId":"55555555-…"` and `"total":62.30`, and these items in this order: `{productId 1, "Classic Logo T-Shirt", 19.90, 2}`, then `{productId 2, "Minimalist Black T-Shirt", 22.50, 1}`. `curl -s "http://localhost:<ordering-port>/orders?buyerId=$B"` lists order `<N>`.
11. **Trace:** in the Aspire dashboard → Traces, the trace for `POST /basket/{buyerId}/checkout` is a single trace that contains basket's server span, basket's HTTP client span `POST` to ordering, ordering's server span `POST /orders`, and ordering's Postgres (`INSERT orderingdb…`) spans. Check it by hand or with Playwright MCP (snapshot of the trace detail).
12. **No Catalog call at checkout:** covered by `CheckoutTests` (no `catalog.test` expectation, and `server.verify()` passes). In the trace from step 11 there is no span to catalog.

## References
- PROJECT.md:
  - §4.2 Basket: `POST /basket/{buyerId}/checkout` returns 201 `{ orderId }`, or 400 if the basket is empty, and the price shown is the price charged;
  - §5 Checkout flow (steps 2–5, and no compensation on failure);
  - §4.3 Ordering `POST /orders` contract;
  - §2 Resilience (errors propagate) and Observability (the checkout trace is the showcase);
  - §7 (the Java service "reference" is the `services__<name>__http__0` env var plus `waitFor`);
  - §8 (`client` layer with `RestClient`, Testcontainers Redis, `MockRestServiceServer` for Catalog and Ordering);
  - §9 (no re-pricing, retries or sagas).
- PLAN.md Phase 3 → Basket, spec 4 (`04-checkout.md`), which is old step 4.2. PLAN 5.4 (checkout page) consumes it.
- Patterns: `microservices/basket/02-add-item.md` (the Catalog client, the peer-URL env var in the AppHost) and `microservices/basket/03-update-item-quantity.md` (test setup and DoD style).
