# catalog 04 — get product

**Status:** Not started

## Goal
Return a single product by id. This is the lookup Basket uses to validate a product and snapshot its name and price when an item is added.

## Depends on
- `catalog/02-seed-products`

## Scope
- [ ] `GET /products/{id}` → `Product`, or 404 when the id doesn't exist.
- [ ] Tests: found and 404.

## Explicit non-goals
- No changes to the list endpoint and no write operations.

## Definition of Done
- `mvn test` passes in catalog.
- `aspire run`: catalog healthy.
- curl against the catalog endpoint URL: `/products/1` returns the product; `/products/99999` returns 404.

## References
