package br.com.guilherme.eshop.ordering.api;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import br.com.guilherme.eshop.ordering.TestcontainersConfiguration;
import br.com.guilherme.eshop.ordering.domain.Order;
import br.com.guilherme.eshop.ordering.repository.OrderRepository;

/**
 * GET /orders?buyerId=. The context and the Postgres container are shared with the other test
 * classes, so every test seeds its own random buyer and never assumes empty tables.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OrderListControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void listsBuyerOrdersNewestFirst() throws Exception {
        UUID buyer = UUID.randomUUID();

        Order older = new Order(buyer, Instant.parse("2026-01-01T10:00:00Z"));
        older.addItem(1, "Mug", new BigDecimal("9.90"), 2);
        older.addItem(2, "Tee", new BigDecimal("19.50"), 1);
        Long olderId = orderRepository.save(older).getId();

        Order newer = new Order(buyer, Instant.parse("2026-01-02T10:00:00Z"));
        newer.addItem(1, "Mug", new BigDecimal("9.90"), 1);
        Long newerId = orderRepository.save(newer).getId();

        Order other = new Order(UUID.randomUUID(), Instant.parse("2026-01-03T10:00:00Z"));
        other.addItem(1, "Mug", new BigDecimal("9.90"), 7);
        orderRepository.save(other);

        mockMvc.perform(get("/orders").param("buyerId", buyer.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(newerId))
                .andExpect(jsonPath("$[0].createdAt").value("2026-01-02T10:00:00Z"))
                .andExpect(jsonPath("$[0].total").value(9.9))
                .andExpect(jsonPath("$[0].itemCount").value(1))
                .andExpect(jsonPath("$[0].items").doesNotExist())
                .andExpect(jsonPath("$[1].id").value(olderId))
                .andExpect(jsonPath("$[1].createdAt").value("2026-01-01T10:00:00Z"))
                .andExpect(jsonPath("$[1].total").value(39.3))
                .andExpect(jsonPath("$[1].itemCount").value(3))
                .andExpect(jsonPath("$[1].items").doesNotExist());
    }

    @Test
    void sameCreatedAtTieBreaksOnIdDescending() throws Exception {
        UUID buyer = UUID.randomUUID();
        Instant same = Instant.parse("2026-01-05T10:00:00Z");

        Order a = new Order(buyer, same);
        a.addItem(1, "Mug", new BigDecimal("9.90"), 1);
        Long aId = orderRepository.save(a).getId();
        Order b = new Order(buyer, same);
        b.addItem(2, "Tee", new BigDecimal("19.50"), 1);
        Long bId = orderRepository.save(b).getId();

        mockMvc.perform(get("/orders").param("buyerId", buyer.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(bId))
                .andExpect(jsonPath("$[1].id").value(aId));
    }

    @Test
    void unknownBuyerReturnsEmptyList() throws Exception {
        mockMvc.perform(get("/orders").param("buyerId", UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void missingBuyerIdIsBadRequest() throws Exception {
        mockMvc.perform(get("/orders")).andExpect(status().isBadRequest());
    }

    @Test
    void invalidBuyerIdIsBadRequest() throws Exception {
        mockMvc.perform(get("/orders").param("buyerId", "not-a-uuid")).andExpect(status().isBadRequest());
    }
}
