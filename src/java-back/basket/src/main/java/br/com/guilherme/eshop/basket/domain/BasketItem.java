package br.com.guilherme.eshop.basket.domain;

import java.math.BigDecimal;

/** One basket line: a snapshot of the product's name and price taken when it was added. */
public record BasketItem(long productId, String productName, BigDecimal unitPrice, int quantity) {
}
