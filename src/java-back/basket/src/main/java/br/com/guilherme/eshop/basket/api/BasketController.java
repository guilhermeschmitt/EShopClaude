package br.com.guilherme.eshop.basket.api;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import br.com.guilherme.eshop.basket.client.CatalogClient;
import br.com.guilherme.eshop.basket.client.CatalogProduct;
import br.com.guilherme.eshop.basket.domain.Basket;
import br.com.guilherme.eshop.basket.repository.BasketRepository;

@RestController
@RequestMapping("/basket")
public class BasketController {

    private final BasketRepository baskets;
    private final CatalogClient catalog;

    public BasketController(BasketRepository baskets, CatalogClient catalog) {
        this.baskets = baskets;
        this.catalog = catalog;
    }

    /** A missing key is an empty basket. A buyerId that is not a UUID fails conversion and returns 400. Never calls Catalog. */
    @GetMapping("/{buyerId}")
    public BasketDto get(@PathVariable UUID buyerId) {
        return BasketDto.from(baskets.find(buyerId).orElseGet(() -> Basket.empty(buyerId)));
    }

    /**
     * Looks the product up in Catalog (404 if unknown), then adds a line with its name and price snapshot,
     * or increments the existing line and refreshes its snapshot. Returns the basket.
     */
    @PostMapping("/{buyerId}/items")
    public BasketDto addItem(@PathVariable UUID buyerId, @RequestBody AddItemRequest request) {
        if (request.productId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "productId is required");
        }
        int quantity = request.quantity() == null ? 1 : request.quantity();
        if (quantity < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "quantity must be >= 1");
        }

        CatalogProduct product = catalog.findProduct(request.productId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "product " + request.productId() + " not found"));

        Basket updated = baskets.find(buyerId).orElseGet(() -> Basket.empty(buyerId))
                .withItem(product.id(), product.name(), product.price(), quantity);
        baskets.save(updated);
        return BasketDto.from(updated);
    }
}
