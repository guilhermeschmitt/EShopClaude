package br.com.guilherme.eshop.ordering.api;

import java.math.BigDecimal;

import br.com.guilherme.eshop.ordering.domain.OrderItem;

/** The API's order line: { productId, productName, unitPrice, quantity }. */
public record OrderItemDto(Long productId, String productName, BigDecimal unitPrice, int quantity) {

    public static OrderItemDto from(OrderItem item) {
        return new OrderItemDto(item.getProductId(), item.getProductName(), item.getUnitPrice(), item.getQuantity());
    }
}
