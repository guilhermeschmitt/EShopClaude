package br.com.guilherme.eshop.basket.client;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/** Looks products up in Catalog. Only 404 is translated; every other failure propagates (no retries, PROJECT §2). */
@Component
public class CatalogClient {

    private final RestClient rest;

    public CatalogClient(RestClient.Builder builder, @Value("${catalog.url}") String catalogUrl) {
        // The builder is shared with OrderingClient (RestClientConfig), so clone it before setting the base URL.
        this.rest = builder.clone().baseUrl(catalogUrl).build();
    }

    public Optional<CatalogProduct> findProduct(long id) {
        try {
            return Optional.ofNullable(rest.get().uri("/products/{id}", id).retrieve().body(CatalogProduct.class));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
    }
}
