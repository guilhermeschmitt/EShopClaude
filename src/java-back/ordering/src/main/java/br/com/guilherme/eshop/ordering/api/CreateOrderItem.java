package br.com.guilherme.eshop.ordering.api;

import java.math.BigDecimal;

/**
 * One line of a POST /orders body, with the price snapshot Basket took. Boxed types so a missing
 * field arrives as null and is rejected instead of silently becoming 0.
 */
public record CreateOrderItem(Long productId, String productName, BigDecimal unitPrice, Integer quantity) {
}
