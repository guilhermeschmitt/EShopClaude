# basket 02 — add item

**Status:** Not started

## Goal
Add a product to the basket. Basket looks the product up in Catalog, rejects unknown ids and stores a snapshot of the name and price. This is the first service-to-service call, so the Basket → Catalog wiring comes with it.

## Depends on
- `basket/01-read-basket`
- `catalog/04-get-product`

## Scope
- [ ] AppHost: basket references + waits for `catalog`; `application.yml` maps `services__catalog__http__0` to `catalog.url`.
- [ ] `client/CatalogClient` (Spring `RestClient`, base URL `catalog.url`) calling `GET /products/{id}`.
- [ ] `POST /basket/{buyerId}/items` `{ productId, quantity=1 }`: 404 if Catalog returns 404; otherwise snapshot name + price and add the line, or increment an existing one. Returns the basket.
- [ ] Tests with `MockRestServiceServer`: new line, increment, unknown product → 404.

## Explicit non-goals
- No quantity update or remove (basket/03), no checkout (basket/04).
- Reading the basket still never calls Catalog.
- No retries or fallbacks.

## Definition of Done
- AppHost build passes.
- `mvn test` passes in basket.
- `aspire run`: every resource healthy.
- curl against the basket endpoint URL: add the same product twice plus another one, then `GET /basket/{buyerId}` shows quantity 2, the snapshot prices and the right total; an unknown product id returns 404.
- Dashboard → Traces: one trace basket → catalog → Postgres for an add.

## References
