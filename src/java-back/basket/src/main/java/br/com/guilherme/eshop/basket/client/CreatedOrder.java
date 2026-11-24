package br.com.guilherme.eshop.basket.client;

/** The part of Ordering's Order response that Basket reads. Other fields are ignored. */
public record CreatedOrder(long id) {
}
