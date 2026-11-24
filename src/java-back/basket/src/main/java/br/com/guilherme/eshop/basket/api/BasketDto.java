package br.com.guilherme.eshop.basket.api;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import br.com.guilherme.eshop.basket.domain.Basket;

public record BasketDto(UUID buyerId, List<BasketItemDto> items, BigDecimal total) {

    public static BasketDto from(Basket basket) {
        return new BasketDto(basket.buyerId(), basket.items().stream().map(BasketItemDto::from).toList(), basket.total());
    }
}
