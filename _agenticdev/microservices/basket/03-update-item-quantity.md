# basket 03 — update item quantity

**Status:** Not started

## Goal
Change the quantity of a basket line, or remove it with quantity 0, so the cart page can adjust the basket.

## Depends on
- `basket/02-add-item`

## Scope
- [ ] `PUT /basket/{buyerId}/items/{productId}` `{ quantity }`: set the quantity; `0` removes the line; 404 if the line doesn't exist. Returns the basket.
- [ ] Tests with Testcontainers Redis: update, remove, 404.

## Explicit non-goals
- No Catalog call on update and no re-pricing.
- No checkout.

## Definition of Done
- `mvn test` passes in basket.
- `aspire run`: every resource healthy.
- curl against the basket endpoint URL on a basket filled through `POST /items`: change a quantity and the total follows; set `0` and the line disappears; a product not in the basket returns 404.

## References
