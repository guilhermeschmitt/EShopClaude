package br.com.guilherme.eshop.basket.api;

/** Body of POST /basket/{buyerId}/items. Boxed so a missing field is detectable; quantity defaults to 1. */
public record AddItemRequest(Long productId, Integer quantity) {
}
