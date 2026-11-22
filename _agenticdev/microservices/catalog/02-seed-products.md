# catalog 02 — seed products

**Status:** Done

## Goal
Load sample products on first start so the catalog has data to browse, and enough of it (about 24 products) that paging is visible.

## Depends on
- `catalog/01-product-schema`

## Scope
- [x] `db/migration/V2__seed_products.sql`: about 24 products across 3–4 categories, with name, description, price and category.
- [x] Test asserts the seeded count and the number of distinct categories.

## Explicit non-goals
- No endpoints.
- No category table: the category stays a plain column.
- No product images.

## Definition of Done
- `mvn test` passes in catalog.
- `aspire run`: catalog healthy.
- pgAdmin → catalogdb: `SELECT category, count(*) FROM product GROUP BY 1` returns 3–4 categories totalling about 24 rows.

## References
