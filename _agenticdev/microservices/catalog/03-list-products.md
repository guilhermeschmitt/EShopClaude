# catalog 03 — list products

**Status:** In progress

## Goal
Let clients browse the catalog: a paged product list that can be filtered by category and searched by name. Filter, search and paging stay in one spec because they are one endpoint with one query and one response shape.

## Depends on
- `catalog/02-seed-products`

## Scope
- [ ] `GET /products?category=&q=&page=0&size=8` → `{ items, page, size, totalItems, totalPages }`.
- [ ] `q` is a case-insensitive match on name, `category` is an exact match, results are ordered by name.
- [ ] `Product` and page DTO records in `api`.
- [ ] Tests with Testcontainers: default page, paging, category filter, search.

## Explicit non-goals
- No `GET /products/{id}` (catalog/04) or `GET /categories` (catalog/05).
- No sort parameters and no admin CRUD.

## Definition of Done
- `mvn test` passes in catalog.
- `aspire run`: catalog healthy.
- curl against the catalog endpoint URL from the dashboard: `/products`, `/products?page=2`, `/products?category=<one>`, `/products?q=<text>` return the expected items and paging fields.
- Dashboard → Traces: each request shows a catalog → Postgres span.

## References
