package br.com.guilherme.eshop.ordering.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * An order placed by a buyer. Items are added only through {@link #addItem}, so the total always
 * equals the sum of unitPrice x quantity over the items.
 */
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "buyer_id", nullable = false)
    private UUID buyerId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "total", nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    protected Order() {
        // for JPA
    }

    public Order(UUID buyerId, Instant createdAt) {
        this.buyerId = buyerId;
        this.createdAt = createdAt;
        this.total = BigDecimal.ZERO.setScale(2);
    }

    public OrderItem addItem(long productId, String productName, BigDecimal unitPrice, int quantity) {
        OrderItem item = new OrderItem(this, productId, productName, unitPrice, quantity);
        items.add(item);
        total = total.add(unitPrice.multiply(BigDecimal.valueOf(quantity)));
        return item;
    }

    public Long getId() {
        return id;
    }

    public UUID getBuyerId() {
        return buyerId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }
}
