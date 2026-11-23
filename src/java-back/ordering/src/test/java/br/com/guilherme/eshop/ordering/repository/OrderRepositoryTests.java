package br.com.guilherme.eshop.ordering.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;

import br.com.guilherme.eshop.ordering.TestcontainersConfiguration;
import br.com.guilherme.eshop.ordering.domain.Order;
import br.com.guilherme.eshop.ordering.domain.OrderItem;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class OrderRepositoryTests {

    @Autowired
    private Flyway flyway;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    void flywayAppliedV1() {
        MigrationInfo current = flyway.info().current();

        assertThat(current).isNotNull();
        assertThat(current.getVersion().getVersion()).isEqualTo("1");
        assertThat(current.getState()).isEqualTo(MigrationState.SUCCESS);
    }

    @Test
    void savesOrderWithItemsAndReadsItBack() {
        UUID buyerId = UUID.randomUUID();
        Instant createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
        Order order = sampleOrder(buyerId, createdAt);

        Long id = orderRepository.save(order).getId();
        assertThat(id).isNotNull();

        transactionTemplate.executeWithoutResult(status -> {
            Order loaded = orderRepository.findById(id).orElseThrow();

            assertThat(loaded.getBuyerId()).isEqualTo(buyerId);
            assertThat(loaded.getCreatedAt()).isEqualTo(createdAt);
            assertThat(loaded.getTotal()).isEqualByComparingTo("39.30");
            assertThat(loaded.getItems())
                    .extracting(OrderItem::getProductId, OrderItem::getProductName, OrderItem::getQuantity)
                    .containsExactlyInAnyOrder(tuple(1L, "Mug", 2), tuple(2L, "Tee", 1));
            assertThat(loaded.getItems())
                    .extracting(OrderItem::getUnitPrice)
                    .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                    .containsExactlyInAnyOrder(new BigDecimal("9.90"), new BigDecimal("19.50"));
        });
    }

    @Test
    void totalIsSumOfLines() {
        Order order = new Order(UUID.randomUUID(), Instant.now());
        assertThat(order.getTotal()).isEqualByComparingTo("0");

        order.addItem(1, "Mug", new BigDecimal("9.90"), 2);
        order.addItem(2, "Tee", new BigDecimal("19.50"), 1);

        assertThat(order.getTotal()).isEqualByComparingTo("39.30");
    }

    @Test
    void findSummariesByBuyerIdOrdersNewestFirstAndSumsQuantities() {
        UUID buyer = UUID.randomUUID();
        UUID otherBuyer = UUID.randomUUID();

        Long first = orderRepository.save(sampleOrder(buyer, Instant.parse("2026-01-01T10:00:00Z"))).getId();
        Order second = new Order(buyer, Instant.parse("2026-01-02T10:00:00Z"));
        second.addItem(1, "Mug", new BigDecimal("9.90"), 1);
        Long secondId = orderRepository.save(second).getId();
        Order third = new Order(buyer, Instant.parse("2026-01-03T10:00:00Z"));
        third.addItem(3, "Cap", new BigDecimal("5.00"), 5);
        Long thirdId = orderRepository.save(third).getId();
        Long otherId = orderRepository.save(sampleOrder(otherBuyer, Instant.parse("2026-01-04T10:00:00Z"))).getId();

        var summaries = orderRepository.findSummariesByBuyerId(buyer);

        assertThat(summaries)
                .extracting(OrderSummary::id, OrderSummary::createdAt, OrderSummary::itemCount)
                .containsExactly(
                        tuple(thirdId, Instant.parse("2026-01-03T10:00:00Z"), 5L),
                        tuple(secondId, Instant.parse("2026-01-02T10:00:00Z"), 1L),
                        tuple(first, Instant.parse("2026-01-01T10:00:00Z"), 3L));
        assertThat(summaries)
                .extracting(OrderSummary::total)
                .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .containsExactly(new BigDecimal("25.00"), new BigDecimal("9.90"), new BigDecimal("39.30"));
        assertThat(summaries).extracting(OrderSummary::id).doesNotContain(otherId);
    }

    private static Order sampleOrder(UUID buyerId, Instant createdAt) {
        Order order = new Order(buyerId, createdAt);
        order.addItem(1, "Mug", new BigDecimal("9.90"), 2);
        order.addItem(2, "Tee", new BigDecimal("19.50"), 1);
        return order;
    }
}
