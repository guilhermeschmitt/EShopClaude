package br.com.guilherme.eshop.ordering.api;

import java.util.List;
import java.util.UUID;

/** POST /orders body: { buyerId, items: [{ productId, productName, unitPrice, quantity }] }. */
public record CreateOrderRequest(UUID buyerId, List<CreateOrderItem> items) {
}
