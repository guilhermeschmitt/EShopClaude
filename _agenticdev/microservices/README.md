# Microservices spec backlog

One row per spec. Rules: [WOW.md](../WOW.md). Update the status when a spec is merged to `main`.

| Service | Spec | Branch | Depends on | Status |
|---|---|---|---|---|
| catalog | [01-product-schema](catalog/01-product-schema.md) | `feature/catalog-01-product-schema` | nothing | Done |
| catalog | [02-seed-products](catalog/02-seed-products.md) | `feature/catalog-02-seed-products` | catalog/01 | Done |
| catalog | [03-list-products](catalog/03-list-products.md) | `feature/catalog-03-list-products` | catalog/02 | Not started |
| catalog | [04-get-product](catalog/04-get-product.md) | `feature/catalog-04-get-product` | catalog/02 | Not started |
| catalog | [05-list-categories](catalog/05-list-categories.md) | `feature/catalog-05-list-categories` | catalog/02 | Not started |
| ordering | [01-order-schema](ordering/01-order-schema.md) | `feature/ordering-01-order-schema` | nothing | Not started |
| ordering | [02-create-order](ordering/02-create-order.md) | `feature/ordering-02-create-order` | ordering/01 | Not started |
| ordering | [03-list-buyer-orders](ordering/03-list-buyer-orders.md) | `feature/ordering-03-list-buyer-orders` | ordering/02 | Not started |
| ordering | [04-get-order](ordering/04-get-order.md) | `feature/ordering-04-get-order` | ordering/02 | Not started |
| basket | [01-read-basket](basket/01-read-basket.md) | `feature/basket-01-read-basket` | nothing | Not started |
| basket | [02-add-item](basket/02-add-item.md) | `feature/basket-02-add-item` | basket/01, catalog/04 | Not started |
| basket | [03-update-item-quantity](basket/03-update-item-quantity.md) | `feature/basket-03-update-item-quantity` | basket/02 | Not started |
| basket | [04-checkout](basket/04-checkout.md) | `feature/basket-04-checkout` | basket/03, ordering/02, ordering/04 | Not started |
