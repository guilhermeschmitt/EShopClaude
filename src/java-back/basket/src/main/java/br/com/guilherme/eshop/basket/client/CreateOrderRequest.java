package br.com.guilherme.eshop.basket.client;

import java.util.List;
import java.util.UUID;

import br.com.guilherme.eshop.basket.domain.BasketItem;

/** Ordering's POST /orders body: { buyerId, items: [{ productId, productName, unitPrice, quantity }] }. */
public record CreateOrderRequest(UUID buyerId, List<CreateOrderItem> items) {

    /** The basket lines as they are, in basket order and with their snapshot prices (no re-pricing). */
    public static CreateOrderRequest from(UUID buyerId, List<BasketItem> items) {
        return new CreateOrderRequest(buyerId, items.stream()
                .map(item -> new CreateOrderItem(item.productId(), item.productName(), item.unitPrice(), item.quantity()))
                .toList());
    }
}
