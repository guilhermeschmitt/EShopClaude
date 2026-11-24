package br.com.guilherme.eshop.basket.client;

import java.math.BigDecimal;

/** One line of Ordering's POST /orders body: the snapshot Basket took when the item was added. */
public record CreateOrderItem(long productId, String productName, BigDecimal unitPrice, int quantity) {
}
