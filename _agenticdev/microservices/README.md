# Microservices spec backlog

One row per spec. Rules: [WOW.md](../WOW.md). Update the status when a spec is merged to `main`.

| Service | Spec | Branch | Depends on | Status |
|---|---|---|---|---|
| catalog | [01-product-schema](catalog/01-product-schema.md) | `feature/catalog-01-product-schema` | nothing | Done |
| catalog | [02-seed-products](catalog/02-seed-products.md) | `feature/catalog-02-seed-products` | catalog/01 | Done |
| catalog | [03-list-products](catalog/03-list-products.md) | `feature/catalog-03-list-products` | catalog/02 | Done |
| catalog | [04-get-product](catalog/04-get-product.md) | `feature/catalog-04-get-product` | catalog/02 | Done |
| catalog | [05-list-categories](catalog/05-list-categories.md) | `feature/catalog-05-list-categories` | catalog/02 | Done |
| ordering | [01-order-schema](ordering/01-order-schema.md) | `feature/ordering-01-order-schema` | nothing | Done (b6357af) |
| ordering | [02-create-order](ordering/02-create-order.md) | `feature/ordering-02-create-order` | ordering/01 | Done (78c10d4) |
| ordering | [03-list-buyer-orders](ordering/03-list-buyer-orders.md) | `feature/ordering-03-list-buyer-orders` | ordering/02 | Done (fb347c9) |
| ordering | [04-get-order](ordering/04-get-order.md) | `feature/ordering-04-get-order` | ordering/02 | Done (df164a2) |
| basket | [01-read-basket](basket/01-read-basket.md) | `feature/basket-01-read-basket` | nothing | Done (00595ad) |
| basket | [02-add-item](basket/02-add-item.md) | `feature/basket-02-add-item` | basket/01, catalog/04 | Done (64a461a) |
| basket | [03-update-item-quantity](basket/03-update-item-quantity.md) | `feature/basket-03-update-item-quantity` | basket/02 | Done (6fe653c) |
| basket | [04-checkout](basket/04-checkout.md) | `feature/basket-04-checkout` | basket/03, ordering/02, ordering/04 | Done (5ee7850) |
| storefront | [01-storefront-shell](storefront/01-storefront-shell.md) | `feature/storefront-01-storefront-shell` | nothing | Done (cbfcc65) |
| storefront | [02-cart-badge](storefront/02-cart-badge.md) | `feature/storefront-02-cart-badge` | storefront/01, basket/01 | Done (049f4be) |
| storefront | [03-product-grid](storefront/03-product-grid.md) | `feature/storefront-03-product-grid` | storefront/01, catalog/03 | Done (5044b4b) |
| storefront | [04-product-pager](storefront/04-product-pager.md) | `feature/storefront-04-product-pager` | storefront/03, catalog/03 | Done (1711aaf) |
| storefront | [05-category-filter](storefront/05-category-filter.md) | `feature/storefront-05-category-filter` | storefront/04, catalog/05 | Done (94728cc) |
| storefront | [06-product-search](storefront/06-product-search.md) | `feature/storefront-06-product-search` | storefront/05, catalog/03 | Done (ca76da0) |
| storefront | [07-add-to-cart](storefront/07-add-to-cart.md) | `feature/storefront-07-add-to-cart` | storefront/02, storefront/03, basket/02 | Done (bd97a18) |
| storefront | [08-cart-view](storefront/08-cart-view.md) | `feature/storefront-08-cart-view` | storefront/02, basket/01 | Done (4dfdc91) |
| storefront | [09-cart-adjust](storefront/09-cart-adjust.md) | `feature/storefront-09-cart-adjust` | storefront/08, basket/03 | Done (236bafb) |
| storefront | [10-checkout-review](storefront/10-checkout-review.md) | `feature/storefront-10-checkout-review` | storefront/08, basket/01 | Done (f6d47ad) |
| storefront | [11-my-orders](storefront/11-my-orders.md) | `feature/storefront-11-my-orders` | storefront/01, ordering/03 | Done (432f626) |
| storefront | [12-order-detail](storefront/12-order-detail.md) | `feature/storefront-12-order-detail` | storefront/11, ordering/04 | Done (5f99e8f) |
| storefront | [13-place-order](storefront/13-place-order.md) | `feature/storefront-13-place-order` | storefront/10, storefront/12, basket/04 | Not started |
