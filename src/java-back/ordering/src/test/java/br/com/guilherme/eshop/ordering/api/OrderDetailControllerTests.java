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
 * GET /orders/{id}. The context and the Postgres container are shared with the other test
 * classes, so every test seeds its own order and looks it up by the generated id.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OrderDetailControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void returnsOrderWithItems() throws Exception {
        UUID buyer = UUID.randomUUID();
        Order order = new Order(buyer, Instant.parse("2026-02-01T10:00:00Z"));
        order.addItem(2, "Tee", new BigDecimal("19.50"), 1);
        order.addItem(1, "Mug", new BigDecimal("9.90"), 2);
        Long id = orderRepository.save(order).getId();

        mockMvc.perform(get("/orders/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.buyerId").value(buyer.toString()))
                .andExpect(jsonPath("$.createdAt").value("2026-02-01T10:00:00Z"))
                .andExpect(jsonPath("$.total").value(39.3))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].productId").value(2))
                .andExpect(jsonPath("$.items[0].productName").value("Tee"))
                .andExpect(jsonPath("$.items[0].unitPrice").value(19.5))
                .andExpect(jsonPath("$.items[0].quantity").value(1))
                .andExpect(jsonPath("$.items[1].productId").value(1))
                .andExpect(jsonPath("$.items[1].productName").value("Mug"))
                .andExpect(jsonPath("$.items[1].unitPrice").value(9.9))
                .andExpect(jsonPath("$.items[1].quantity").value(2));
    }

    @Test
    void unknownIdIsNotFound() throws Exception {
        mockMvc.perform(get("/orders/9223372036854775807")).andExpect(status().isNotFound());
    }

    @Test
    void nonNumericIdIsBadRequest() throws Exception {
        mockMvc.perform(get("/orders/abc")).andExpect(status().isBadRequest());
    }
}
