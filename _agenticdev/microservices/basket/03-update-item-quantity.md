# basket 03 — update item quantity

**Status:** Done (6fe653c)

## Goal
Change the quantity of a basket line, or remove the line with quantity 0, so the cart page can adjust the basket. The update uses only the snapshot already in Redis. It never calls Catalog and never re-prices.

## Context
- basket/02 is merged (64a461a). `src/apphost.mts` registers `basket` with Redis (`SPRING_DATA_REDIS_HOST/PORT/PASSWORD`), `services__catalog__http__0` from `catalog.getEndpoint('http')` and `waitFor(redis)` and `waitFor(catalog)`. This spec doesn't change the AppHost.
- Basket code today:
  - `domain.Basket(UUID buyerId, List<BasketItem> items)` has `empty()`, `withItem(...)` and a computed `total()` (scale 2). Its compact constructor copies `items` with `List.copyOf`.
  - `domain.BasketItem(long productId, String productName, BigDecimal unitPrice, int quantity)`.
  - `repository.BasketRepository` has `key(UUID)`, `find(UUID)` and `save(Basket)` (JSON under `basket:{buyerId}`, no TTL). There is no delete method.
  - `api.BasketController` has `GET /basket/{buyerId}` and `POST /basket/{buyerId}/items`. Validation is done by hand with `ResponseStatusException`, and there is no Bean Validation. A buyerId that isn't a UUID already fails conversion with a 400.
  - `api.AddItemRequest(Long productId, Integer quantity)`, `api.BasketDto` and `api.BasketItemDto`.
  - `client.CatalogClient` and `client.CatalogProduct` (not used by this spec).
- Tests today, 20 in total:
  - `BasketApplicationTests` (2)
  - `BasketTests` (4)
  - `BasketRepositoryTests` (3)
  - `BasketControllerTests` (3)
  - `AddItemTests` (6)

  `AddItemTests` shows the pattern: `@SpringBootTest`, `@AutoConfigureMockMvc`, `@AutoConfigureMockRestServiceServer` and `@Import(TestcontainersConfiguration.class)`. `src/test/resources/config/application.yml` sets `catalog.url: http://catalog.test`.

## Depends on
- `basket/02-add-item`

## Scope
- [ ] `PUT /basket/{buyerId}/items/{productId}` with the body `{ quantity }`:
  - Validation runs before Redis is read. A non-UUID `buyerId` → 400. A missing or null `quantity` → 400. `quantity < 0` → 400 (decided).
  - If there is no basket for the buyer, or the basket has no line for `productId`, the response is 404 and nothing is written. No key is created (decided).
  - `quantity > 0` sets the line's quantity. The line keeps its position, name and price.
  - `quantity == 0` removes the line. If that was the last line, the basket is saved with `items: []` and the key stays (decided).
  - Saves, then returns **200** with the basket in the same shape as `GET` (decided).
- [ ] Domain and controller tests, with Testcontainers Redis and `MockRestServiceServer` proving that Catalog is never called.

## Explicit non-goals
- No Catalog call on update and no re-pricing (PROJECT §4.2, §9).
- No checkout, no `ordering.url` and no Ordering client (basket/04).
- No `DELETE` endpoint (removing a line is `PUT` with `0`), no repository delete method, and no upper limit on quantity.
- No Bean Validation dependency.
- No locking or optimistic concurrency on the Redis read-modify-write, which is accepted for the demo as in basket/02.
- No changes to `src/apphost.mts`, `pom.xml`, `application.yml`, catalog, ordering or the storefront.

## Data model
- **Redis:** unchanged. The key is `basket:{buyerId}`, the JSON is `{ buyerId, items: [{ productId, productName, unitPrice, quantity }] }`, and there is no TTL. A basket whose last line was removed is stored as `{ buyerId, items: [] }`.
- **Domain** `Basket`: add `Optional<Basket> withQuantity(long productId, int quantity)`.
  - If there is no line for `productId`, it returns `Optional.empty()`.
  - If `quantity == 0`, it returns a basket without that line, and the other lines keep their order.
  - If `quantity > 0`, it returns a basket where that line is replaced in the same position by `(productId, productName, unitPrice, quantity)`.
  - Validating that `quantity` is not negative belongs to the controller.
- **API** (`api` package):
  - `UpdateItemRequest(Integer quantity)` is a record with a boxed field, so a missing value is detectable.
  - `BasketController` gets `@PutMapping("/{buyerId}/items/{productId}")`, with `@PathVariable UUID buyerId`, `@PathVariable long productId` and `@RequestBody UpdateItemRequest`, and it returns `BasketDto`.
    - A missing or negative quantity throws `ResponseStatusException(BAD_REQUEST, …)`.
    - A missing basket or line throws `ResponseStatusException(NOT_FOUND, "product <id> not in basket")`.
- **How the data store is created:** nothing to create. Redis is schemaless, and no env vars change.

## Tasks
1. **Domain** (`domain/Basket.java`): add `withQuantity(...)` as described in Data model.
2. **API:**
   - Add `api/UpdateItemRequest.java`.
   - Add the `PUT` handler in `api/BasketController.java`. Its order is: validate quantity → `baskets.find(buyerId)` → `flatMap(b -> b.withQuantity(productId, quantity))` → if empty, 404 → `baskets.save` → `BasketDto.from`.
3. **Domain tests** (`domain/BasketTests.java`) get three new tests:
   - `withQuantitySetsLineQuantity`: `[(1,"Mug",9.90,2),(2,"Tee",19.50,1)]` with `withQuantity(1, 5)` gives line 0 = `(1,"Mug",9.90,5)`, line 1 unchanged and a total of `69.00`.
   - `withQuantityZeroRemovesLine`: the same basket with `withQuantity(1, 0)` gives only `(2,"Tee",19.50,1)` and a total of `19.50`.
   - `withQuantityUnknownLineIsEmpty`: the same basket with `withQuantity(99, 1)` gives `Optional.empty()`.
4. **Controller tests:** a new `api/UpdateItemTests.java`, set up like `AddItemTests`:
   - It uses `@SpringBootTest`, `@AutoConfigureMockMvc`, `@AutoConfigureMockRestServiceServer` and `@Import(TestcontainersConfiguration.class)`.
   - `@BeforeEach` resets the mock server and deletes every `basket:*` key.
   - Every test ends with `catalog.verify()` and no expectations, which proves Catalog is never called.
   - Stored basket = `[(1,"Mug",9.90,2),(2,"Tee",19.50,1)]`.
   - The tests:
     - `updateSetsQuantityAndTotal`: `PUT .../items/1` `{"quantity":5}` → 200. `$.items[0].quantity` is 5, `$.items[0].unitPrice` is 9.90, `$.items.length()` is 2 and `$.total` is 69.00. `GET` afterwards shows the same.
     - `zeroRemovesLine`: `PUT .../items/1` `{"quantity":0}` → 200. `$.items.length()` is 1, `$.items[0].productId` is 2 and `$.total` is 19.50. Then `PUT .../items/2` `{"quantity":0}` → 200, `$.items.length()` is 0 and `$.total` is 0. The Redis key still exists.
     - `unknownLineReturns404`: `PUT .../items/99` `{"quantity":1}` → 404, and the stored JSON is unchanged.
     - `missingBasketReturns404`: for a random buyer with no key, `PUT .../items/1` `{"quantity":1}` → 404, and the key does not exist afterwards.
     - `negativeQuantityReturns400`: `{"quantity":-1}` → 400, and the stored JSON is unchanged.
     - `missingQuantityReturns400`: `{}` → 400.
5. Existing tests are unchanged and still pass.

## Files touched
- `src/java-back/basket/src/main/java/br/com/guilherme/eshop/basket/domain/Basket.java` (edit)
- `src/java-back/basket/src/main/java/br/com/guilherme/eshop/basket/api/UpdateItemRequest.java` (new)
- `src/java-back/basket/src/main/java/br/com/guilherme/eshop/basket/api/BasketController.java` (edit)
- `src/java-back/basket/src/test/java/br/com/guilherme/eshop/basket/domain/BasketTests.java` (edit)
- `src/java-back/basket/src/test/java/br/com/guilherme/eshop/basket/api/UpdateItemTests.java` (new)

The AppHost, catalog, ordering and the storefront don't change.

## Definition of Done
Run every check from the repository root in Git Bash unless it says otherwise.
- `<basket-port>` is basket's HTTP port from `aspire describe`.
- `<redis-cli>` is `docker exec -i <redis-container> sh -c 'redis-cli -a "$REDIS_PASSWORD" --no-auth-warning'`, followed by the command, as in basket/01.
- `B` is `33333333-3333-4333-8333-333333333333`.
- `PUT` is `curl -s -i -X PUT -H 'Content-Type: application/json'`.

1. **AppHost build:** in `src/`, `npm run build` exits 0.
2. **Tests:** with Docker running, `mvn -f src/java-back/basket/pom.xml test` prints `BUILD SUCCESS` with 0 failures and 0 errors. The report lists:
   - `BasketApplicationTests` (2)
   - `BasketTests` (7)
   - `BasketRepositoryTests` (3)
   - `BasketControllerTests` (3)
   - `AddItemTests` (6)
   - `UpdateItemTests` (6)
3. **Image:** `docker build src/java-back/basket` exits 0.
4. **Healthy start:** in `src/`, `aspire run` brings up postgres, catalogdb, orderingdb, pgAdmin, redis, catalog, basket and ordering. `aspire wait <resource>` exits 0 for each one, and `aspire describe` shows each as Running/Healthy.
5. **Fill the basket:**
   - `curl -s -X POST -H 'Content-Type: application/json' -d '{"productId":1}' http://localhost:<basket-port>/basket/$B/items` → 200.
   - Then the same request with `-d '{"productId":2}'` → 200, with two items and `"total":42.40` (19.90 + 22.50).
6. **Change a quantity:** `PUT -d '{"quantity":3}' http://localhost:<basket-port>/basket/$B/items/1` → `200`. Item 1 has `"quantity":3` and `"unitPrice":19.90`, item 2 is unchanged, and `"total":82.20`.
7. **Read back:** `curl -s http://localhost:<basket-port>/basket/$B` → 200 with the same lines and `"total":82.20`.
8. **Remove a line:** `PUT -d '{"quantity":0}' .../basket/$B/items/2` → `200`. There is one item (`productId` 1) and `"total":59.70`.
9. **Not in basket:** `PUT -d '{"quantity":1}' .../basket/$B/items/2` → `404`. `PUT -d '{"quantity":1}' .../basket/44444444-4444-4444-8444-444444444444/items/1` → `404`, and `<redis-cli> EXISTS basket:44444444-4444-4444-8444-444444444444` returns `0`.
10. **Bad input:**
    - `PUT -d '{"quantity":-1}' .../basket/$B/items/1` → `400`.
    - `PUT -d '{}' .../basket/$B/items/1` → `400`.
    - `PUT -d '{"quantity":1}' .../basket/not-a-uuid/items/1` → `400`.
    - Then `curl .../basket/$B` still shows `"total":59.70`.
11. **Remove last line:** `PUT -d '{"quantity":0}' .../basket/$B/items/1` → `200` with `"items":[]` and `"total":0` (rendered as `0` or `0.00`). `<redis-cli> GET basket:$B` returns `{"buyerId":"33333333-…","items":[]}`, and `<redis-cli> TTL basket:$B` returns `-1`. Clean up with `<redis-cli> DEL basket:$B` → `1`.
12. **No Catalog call:** this is covered by `UpdateItemTests` (`catalog.verify()` with no expectations in every test).

## References
- PROJECT.md:
  - §4.2 Basket: `PUT /basket/{buyerId}/items/{productId}` `{ quantity }` sets the quantity, `0` removes the line, it returns the basket or 404 if the line doesn't exist, and the price shown is the price charged;
  - §2 Resilience (errors propagate);
  - §8 (layers, Testcontainers Redis, `MockRestServiceServer`);
  - §9 (no re-pricing, no TTL).
- PLAN.md Phase 3 → Basket, spec 3 (`03-update-item-quantity.md`), part of old step 3.4. PLAN 5.3 (cart page) consumes it.
- Pattern: `microservices/basket/02-add-item.md` (Done) for the manual validation, the test setup and the DoD style.
