package br.com.guilherme.eshop.ordering.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import br.com.guilherme.eshop.ordering.domain.Order;

/** The API's Order: { id, buyerId, createdAt, total, items[] }. Items keep the order they were added in. */
public record OrderDto(Long id, UUID buyerId, Instant createdAt, BigDecimal total, List<OrderItemDto> items) {

    public static OrderDto from(Order order) {
        return new OrderDto(order.getId(), order.getBuyerId(), order.getCreatedAt(), order.getTotal(),
                order.getItems().stream().map(OrderItemDto::from).toList());
    }
}
