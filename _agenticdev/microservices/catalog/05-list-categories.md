# catalog 05 — list categories

**Status:** In progress

## Goal
Return the distinct product categories so the storefront can offer a category filter.

## Depends on
- `catalog/02-seed-products`

## Scope
- [ ] `GET /categories` → `string[]`, distinct and sorted.
- [ ] Test asserts the seeded categories in sorted order.

## Explicit non-goals
- No category table, no counts per category.

## Definition of Done
- `mvn test` passes in catalog.
- `aspire run`: catalog healthy.
- curl against the catalog endpoint URL: `/categories` returns the 3–4 seeded categories, sorted.

## References
