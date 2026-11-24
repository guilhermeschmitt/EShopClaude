package br.com.guilherme.eshop.basket.client;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import br.com.guilherme.eshop.basket.domain.BasketItem;

/** Creates orders in Ordering at checkout. Nothing is translated: every failure propagates (no retries, PROJECT §2). */
@Component
public class OrderingClient {

    private final RestClient rest;

    public OrderingClient(RestClient.Builder builder, @Value("${ordering.url}") String orderingUrl) {
        // The builder is shared with CatalogClient (RestClientConfig), so clone it before setting the base URL.
        this.rest = builder.clone().baseUrl(orderingUrl).build();
    }

    /** POSTs the basket lines with their snapshot prices and returns the created order's id. */
    public long createOrder(UUID buyerId, List<BasketItem> items) {
        return rest.post()
                .uri("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .body(CreateOrderRequest.from(buyerId, items))
                .retrieve()
                .body(CreatedOrder.class)
                .id();
    }
}
