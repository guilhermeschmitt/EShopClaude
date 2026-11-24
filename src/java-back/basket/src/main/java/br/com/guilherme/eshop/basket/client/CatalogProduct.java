package br.com.guilherme.eshop.basket.client;

import java.math.BigDecimal;

/** The part of Catalog's Product that Basket snapshots. Other fields (description, category) are ignored. */
public record CatalogProduct(long id, String name, BigDecimal price) {
}
