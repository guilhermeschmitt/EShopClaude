package br.com.guilherme.eshop.basket.api;

import java.math.BigDecimal;

import br.com.guilherme.eshop.basket.domain.BasketItem;

public record BasketItemDto(long productId, String productName, BigDecimal unitPrice, int quantity) {

    static BasketItemDto from(BasketItem item) {
        return new BasketItemDto(item.productId(), item.productName(), item.unitPrice(), item.quantity());
    }
}
