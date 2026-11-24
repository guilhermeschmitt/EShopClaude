package br.com.guilherme.eshop.basket.api;

/** Body of PUT /basket/{buyerId}/items/{productId}. Boxed so a missing quantity is detectable; 0 removes the line. */
public record UpdateItemRequest(Integer quantity) {
}
