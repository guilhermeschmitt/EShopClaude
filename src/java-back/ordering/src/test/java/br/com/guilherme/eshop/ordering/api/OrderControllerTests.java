package br.com.guilherme.eshop.ordering.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;

import br.com.guilherme.eshop.ordering.TestcontainersConfiguration;
import br.com.guilherme.eshop.ordering.domain.Order;
import br.com.guilherme.eshop.ordering.repository.OrderRepository;

/**
 * POST /orders. The context and the Postgres container are shared with the other test classes, so
 * persistence is checked with count() deltas, never absolute counts.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OrderControllerTests {

    private static final String BUYER = "11111111-1111-1111-1111-111111111111";
    private static final String NOW = "2026-01-15T10:00:00Z";

    private static final String MUG = line("1", "\"Mug\"", "9.90", "2");
    private static final String TEE = line("2", "\"Tee\"", "19.50", "1");

    @TestBean
    private Clock clock;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    static Clock clock() {
        return Clock.fixed(Instant.parse(NOW), ZoneOffset.UTC);
    }

    @Test
    void createsOrderAndComputesTotal() throws Exception {
        long before = orderRepository.count();

        String body = postOrder(order("\"" + BUYER + "\"", MUG + "," + TEE))
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.buyerId").value(BUYER))
                .andExpect(jsonPath("$.createdAt").value(NOW))
                .andExpect(jsonPath("$.total").value(39.30))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].productId").value(1))
                .andExpect(jsonPath("$.items[0].productName").value("Mug"))
                .andExpect(jsonPath("$.items[0].unitPrice").value(9.90))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[1].productId").value(2))
                .andExpect(jsonPath("$.items[1].productName").value("Tee"))
                .andExpect(jsonPath("$.items[1].unitPrice").value(19.50))
                .andExpect(jsonPath("$.items[1].quantity").value(1))
                .andReturn().getResponse().getContentAsString();

        assertThat(orderRepository.count()).isEqualTo(before + 1);

        long id = Long.parseLong(body.replaceAll("(?s).*\"id\"\\s*:\\s*(\\d+).*", "$1"));
        transactionTemplate.executeWithoutResult(tx -> {
            Order saved = orderRepository.findById(id).orElseThrow();
            assertThat(saved.getBuyerId()).hasToString(BUYER);
            assertThat(saved.getCreatedAt()).isEqualTo(Instant.parse(NOW));
            assertThat(saved.getTotal()).isEqualByComparingTo(new BigDecimal("39.30"));
            assertThat(saved.getItems()).hasSize(2);
        });
    }

    @Test
    void emptyItemsIsBadRequest() throws Exception {
        postOrder(order("\"" + BUYER + "\"", "")).andExpect(status().isBadRequest());
    }

    @Test
    void missingItemsIsBadRequest() throws Exception {
        postOrder("{\"buyerId\":\"" + BUYER + "\"}").andExpect(status().isBadRequest());
    }

    @Test
    void zeroQuantityIsBadRequest() throws Exception {
        postOrder(order("\"" + BUYER + "\"", line("1", "\"Mug\"", "9.90", "0"))).andExpect(status().isBadRequest());
    }

    @Test
    void negativeQuantityIsBadRequest() throws Exception {
        postOrder(order("\"" + BUYER + "\"", line("1", "\"Mug\"", "9.90", "-1"))).andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @MethodSource("invalidBodies")
    void invalidFieldsAreBadRequest(String body) throws Exception {
        postOrder(body).andExpect(status().isBadRequest());
    }

    @Test
    void malformedBodyIsBadRequest() throws Exception {
        postOrder(order("\"not-a-uuid\"", MUG)).andExpect(status().isBadRequest());
        postOrder("{\"buyerId\":").andExpect(status().isBadRequest());
    }

    @Test
    void rejectedRequestsPersistNothing() throws Exception {
        long before = orderRepository.count();

        postOrder(order("\"" + BUYER + "\"", "")).andExpect(status().isBadRequest());
        postOrder(order("\"" + BUYER + "\"", MUG + "," + line("2", "\"Tee\"", "19.50", "0")))
                .andExpect(status().isBadRequest());
        postOrder(order("\"" + BUYER + "\"", MUG + "," + line("2", "\"Tee\"", "9.999", "1")))
                .andExpect(status().isBadRequest());
        postOrder(order("\"" + BUYER + "\"", line("1", "\"Big\"", "99999999.99", "2")))
                .andExpect(status().isBadRequest());

        assertThat(orderRepository.count()).isEqualTo(before);
    }

    static Stream<String> invalidBodies() {
        String buyer = "\"" + BUYER + "\"";
        return Stream.of(
                // buyerId missing
                "{\"items\":[" + MUG + "]}",
                // null item
                order(buyer, "null"),
                // productId missing
                order(buyer, "{\"productName\":\"Mug\",\"unitPrice\":9.90,\"quantity\":1}"),
                // productName missing, blank, too long
                order(buyer, "{\"productId\":1,\"unitPrice\":9.90,\"quantity\":1}"),
                order(buyer, line("1", "\"  \"", "9.90", "1")),
                order(buyer, line("1", "\"" + "x".repeat(101) + "\"", "9.90", "1")),
                // unitPrice missing, negative, three decimals, above numeric(10,2)
                order(buyer, "{\"productId\":1,\"productName\":\"Mug\",\"quantity\":1}"),
                order(buyer, line("1", "\"Mug\"", "-0.01", "1")),
                order(buyer, line("1", "\"Mug\"", "9.999", "1")),
                order(buyer, line("1", "\"Mug\"", "100000000.00", "1")),
                // quantity missing
                order(buyer, "{\"productId\":1,\"productName\":\"Mug\",\"unitPrice\":9.90}"),
                // total above numeric(10,2)
                order(buyer, line("1", "\"Big\"", "99999999.99", "2")));
    }

    private ResultActions postOrder(String body) throws Exception {
        return mockMvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static String order(String buyerJson, String itemsJson) {
        return "{\"buyerId\":" + buyerJson + ",\"items\":[" + itemsJson + "]}";
    }

    private static String line(String productId, String productNameJson, String unitPrice, String quantity) {
        return "{\"productId\":" + productId + ",\"productName\":" + productNameJson
                + ",\"unitPrice\":" + unitPrice + ",\"quantity\":" + quantity + "}";
    }
}
