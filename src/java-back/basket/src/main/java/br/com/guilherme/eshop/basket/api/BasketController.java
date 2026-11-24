package br.com.guilherme.eshop.basket.api;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.guilherme.eshop.basket.domain.Basket;
import br.com.guilherme.eshop.basket.repository.BasketRepository;

@RestController
@RequestMapping("/basket")
public class BasketController {

    private final BasketRepository baskets;

    public BasketController(BasketRepository baskets) {
        this.baskets = baskets;
    }

    /** A missing key is an empty basket. A buyerId that is not a UUID fails conversion and returns 400. */
    @GetMapping("/{buyerId}")
    public BasketDto get(@PathVariable UUID buyerId) {
        return BasketDto.from(baskets.find(buyerId).orElseGet(() -> Basket.empty(buyerId)));
    }
}
