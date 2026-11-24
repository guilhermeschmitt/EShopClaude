package br.com.guilherme.eshop.basket.api;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import br.com.guilherme.eshop.basket.client.CatalogClient;
import br.com.guilherme.eshop.basket.client.CatalogProduct;
import br.com.guilherme.eshop.basket.client.OrderingClient;
import br.com.guilherme.eshop.basket.domain.Basket;
import br.com.guilherme.eshop.basket.repository.BasketRepository;

@RestController
@RequestMapping("/basket")
public class BasketController {

    private final BasketRepository baskets;
    private final CatalogClient catalog;
    private final OrderingClient ordering;

    public BasketController(BasketRepository baskets, CatalogClient catalog, OrderingClient ordering) {
        this.baskets = baskets;
        this.catalog = catalog;
        this.ordering = ordering;
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

    /**
     * Sets a line's quantity from the stored snapshot (no Catalog call, no re-pricing); 0 removes the line.
     * 404 when the basket or the line doesn't exist, and nothing is written.
     */
    @PutMapping("/{buyerId}/items/{productId}")
    public BasketDto updateItem(@PathVariable UUID buyerId, @PathVariable long productId,
            @RequestBody UpdateItemRequest request) {
        if (request.quantity() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "quantity is required");
        }
        int quantity = request.quantity();
        if (quantity < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "quantity must be >= 0");
        }

        Basket updated = baskets.find(buyerId)
                .flatMap(basket -> basket.withQuantity(productId, quantity))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "product " + productId + " not in basket"));
        baskets.save(updated);
        return BasketDto.from(updated);
    }

    /**
     * Turns the basket into an order (PROJECT §5): sends its lines with the snapshot prices to Ordering (no Catalog
     * call, no re-pricing) and deletes the basket only after the order exists. 400 when the basket is missing or
     * empty. Any Ordering failure propagates as a 500 and the basket is kept, so the buyer can retry.
     */
    @PostMapping("/{buyerId}/checkout")
    public ResponseEntity<CheckoutResponse> checkout(@PathVariable UUID buyerId) {
        Basket basket = baskets.find(buyerId)
                .filter(b -> !b.items().isEmpty())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "basket is empty"));

        long orderId = ordering.createOrder(buyerId, basket.items());
        baskets.delete(buyerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(new CheckoutResponse(orderId));
    }
}
