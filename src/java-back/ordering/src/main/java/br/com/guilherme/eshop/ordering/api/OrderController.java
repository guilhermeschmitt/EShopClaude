package br.com.guilherme.eshop.ordering.api;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import br.com.guilherme.eshop.ordering.domain.Order;
import br.com.guilherme.eshop.ordering.repository.OrderRepository;

@RestController
public class OrderController {

    /** The largest value a numeric(10,2) column holds. */
    static final BigDecimal MAX_AMOUNT = new BigDecimal("99999999.99");

    static final int MAX_PRODUCT_NAME_LENGTH = 100;

    private final OrderRepository orderRepository;
    private final Clock clock;

    public OrderController(OrderRepository orderRepository, Clock clock) {
        this.orderRepository = orderRepository;
        this.clock = clock;
    }

    /**
     * Persists an order sent by Basket at checkout. The prices are trusted as sent; the total is
     * computed as the sum of unitPrice x quantity. 400 on any invalid field, before anything is saved.
     */
    @PostMapping("/orders")
    public ResponseEntity<OrderDto> create(@RequestBody CreateOrderRequest request) {
        validate(request);

        // timestamptz keeps microseconds; truncate so the 201 body matches what is stored.
        Order order = new Order(request.buyerId(), Instant.now(clock).truncatedTo(ChronoUnit.MICROS));
        for (CreateOrderItem item : request.items()) {
            order.addItem(item.productId(), item.productName(), item.unitPrice(), item.quantity());
        }
        if (order.getTotal().compareTo(MAX_AMOUNT) > 0) {
            throw badRequest("total must be <= " + MAX_AMOUNT);
        }

        Order saved = orderRepository.save(order);
        return ResponseEntity.status(HttpStatus.CREATED).body(OrderDto.from(saved));
    }

    /**
     * A buyer's orders as summaries, newest first, not paged. buyerId is required: a missing or
     * non-UUID value is Spring's default 400. A buyer with no orders gets [].
     */
    @GetMapping("/orders")
    public List<OrderSummaryDto> list(@RequestParam UUID buyerId) {
        return orderRepository.findSummariesByBuyerId(buyerId).stream().map(OrderSummaryDto::from).toList();
    }

    private static void validate(CreateOrderRequest request) {
        if (request.buyerId() == null) {
            throw badRequest("buyerId is required");
        }
        if (request.items() == null || request.items().isEmpty()) {
            throw badRequest("items must not be empty");
        }
        for (CreateOrderItem item : request.items()) {
            if (item == null) {
                throw badRequest("items must not contain null");
            }
            if (item.productId() == null) {
                throw badRequest("productId is required");
            }
            if (item.productName() == null || item.productName().isBlank()
                    || item.productName().length() > MAX_PRODUCT_NAME_LENGTH) {
                throw badRequest("productName must be 1 to " + MAX_PRODUCT_NAME_LENGTH + " characters");
            }
            BigDecimal unitPrice = item.unitPrice();
            if (unitPrice == null || unitPrice.signum() < 0 || unitPrice.stripTrailingZeros().scale() > 2
                    || unitPrice.compareTo(MAX_AMOUNT) > 0) {
                throw badRequest("unitPrice must be between 0 and " + MAX_AMOUNT + " with at most 2 decimals");
            }
            if (item.quantity() == null || item.quantity() <= 0) {
                throw badRequest("quantity must be > 0");
            }
        }
    }

    private static ResponseStatusException badRequest(String reason) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);
    }
}
