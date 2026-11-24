package br.com.guilherme.eshop.basket.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

/** A buyer's basket as stored in Redis. The total is always computed, never stored. */
public record Basket(UUID buyerId, List<BasketItem> items) {

    public Basket {
        items = items == null ? List.of() : List.copyOf(items);
    }

    public static Basket empty(UUID buyerId) {
        return new Basket(buyerId, List.of());
    }

    /** Sum of unitPrice x quantity, scale 2. Not a JavaBean getter, so Jackson never writes it to Redis. */
    public BigDecimal total() {
        return items.stream()
                .map(item -> item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
