package br.com.guilherme.eshop.catalog.api;

import java.math.BigDecimal;

import br.com.guilherme.eshop.catalog.domain.Product;

/** The API's Product: { id, name, description, price, category }. */
public record ProductDto(Long id, String name, String description, BigDecimal price, String category) {

    public static ProductDto from(Product product) {
        return new ProductDto(product.getId(), product.getName(), product.getDescription(),
                product.getPrice(), product.getCategory());
    }
}
