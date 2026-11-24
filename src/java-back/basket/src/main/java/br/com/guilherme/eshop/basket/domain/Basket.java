package br.com.guilherme.eshop.basket.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** A buyer's basket as stored in Redis. The total is always computed, never stored. */
public record Basket(UUID buyerId, List<BasketItem> items) {

    public Basket {
        items = items == null ? List.of() : List.copyOf(items);
    }

    public static Basket empty(UUID buyerId) {
        return new Basket(buyerId, List.of());
    }

    /**
     * Adds quantity to the line for productId, refreshing its name and price snapshot and keeping its position,
     * or appends a new line.
     */
    public Basket withItem(long productId, String productName, BigDecimal unitPrice, int quantity) {
        List<BasketItem> updated = new ArrayList<>(items);
        for (int i = 0; i < updated.size(); i++) {
            BasketItem existing = updated.get(i);
            if (existing.productId() == productId) {
                updated.set(i, new BasketItem(productId, productName, unitPrice, existing.quantity() + quantity));
                return new Basket(buyerId, updated);
            }
        }
        updated.add(new BasketItem(productId, productName, unitPrice, quantity));
        return new Basket(buyerId, updated);
    }

    /**
     * Sets the quantity of the line for productId, keeping its snapshot and position; 0 removes the line.
     * Empty when the basket has no line for productId. The caller rejects negative quantities.
     */
    public Optional<Basket> withQuantity(long productId, int quantity) {
        List<BasketItem> updated = new ArrayList<>(items);
        for (int i = 0; i < updated.size(); i++) {
            BasketItem existing = updated.get(i);
            if (existing.productId() == productId) {
                if (quantity == 0) {
                    updated.remove(i);
                } else {
                    updated.set(i, new BasketItem(productId, existing.productName(), existing.unitPrice(), quantity));
                }
                return Optional.of(new Basket(buyerId, updated));
            }
        }
        return Optional.empty();
    }

    /** Sum of unitPrice x quantity, scale 2. Not a JavaBean getter, so Jackson never writes it to Redis. */
    public BigDecimal total() {
        return items.stream()
                .map(item -> item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
