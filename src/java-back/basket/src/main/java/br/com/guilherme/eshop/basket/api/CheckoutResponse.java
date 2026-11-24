package br.com.guilherme.eshop.basket.api;

/** POST /basket/{buyerId}/checkout response: the id of the order Ordering created. */
public record CheckoutResponse(long orderId) {
}
