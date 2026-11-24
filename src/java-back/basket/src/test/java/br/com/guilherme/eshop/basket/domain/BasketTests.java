package br.com.guilherme.eshop.basket.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class BasketTests {

    @Test
    void emptyBasketTotalIsZero() {
        assertThat(Basket.empty(UUID.randomUUID()).total()).isEqualTo(new BigDecimal("0.00"));
    }

    @Test
    void totalIsSumOfLines() {
        Basket basket = new Basket(UUID.randomUUID(), List.of(
                new BasketItem(1, "Mug", new BigDecimal("9.90"), 2),
                new BasketItem(2, "Tee", new BigDecimal("19.50"), 1)));

        assertThat(basket.total()).isEqualTo(new BigDecimal("39.30"));
    }
}
