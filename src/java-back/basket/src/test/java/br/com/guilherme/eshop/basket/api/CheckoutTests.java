package br.com.guilherme.eshop.basket.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.ConnectException;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.test.autoconfigure.AutoConfigureMockRestServiceServer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClientException;

import br.com.guilherme.eshop.basket.TestcontainersConfiguration;
import br.com.guilherme.eshop.basket.repository.BasketRepository;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureMockRestServiceServer
@Import(TestcontainersConfiguration.class)
class CheckoutTests {

    @Autowired
    private MockMvc mockMvc;

    /** One server stubs both peers (shared RestClient.Builder). No test expects catalog.test, so Catalog is never called. */
    @Autowired
    private MockRestServiceServer server;

    @Autowired
    private StringRedisTemplate redis;

    @BeforeEach
    void reset() {
        server.reset();
        Set<String> keys = redis.keys("basket:*");
        if (!keys.isEmpty()) {
            redis.delete(keys);
        }
    }

    @AfterEach
    void onlyExpectedCalls() {
        server.verify();
    }

    private String storeMugAndTee(UUID buyerId) {
        String json = "{\"buyerId\":\"" + buyerId + "\",\"items\":["
                + "{\"productId\":1,\"productName\":\"Mug\",\"unitPrice\":9.90,\"quantity\":2},"
                + "{\"productId\":2,\"productName\":\"Tee\",\"unitPrice\":19.50,\"quantity\":1}]}";
        redis.opsForValue().set(BasketRepository.key(buyerId), json);
        return json;
    }

    private ResponseActions expectCreateOrder(UUID buyerId) {
        return server.expect(requestTo("http://ordering.test/orders"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"buyerId\":\"" + buyerId + "\",\"items\":["
                        + "{\"productId\":1,\"productName\":\"Mug\",\"unitPrice\":9.90,\"quantity\":2},"
                        + "{\"productId\":2,\"productName\":\"Tee\",\"unitPrice\":19.50,\"quantity\":1}]}", true));
    }

    /** MockMvc rethrows an unhandled exception instead of rendering Spring's 500; the real 500 is checked in the DoD. */
    private void assertCheckoutFailsAndKeepsBasket(UUID buyerId, String storedJson) {
        assertThatThrownBy(() -> mockMvc.perform(post("/basket/{buyerId}/checkout", buyerId)))
                .hasCauseInstanceOf(RestClientException.class);
        assertThat(redis.opsForValue().get(BasketRepository.key(buyerId))).isEqualTo(storedJson);
    }

    @Test
    void checkoutCreatesOrderAndClearsBasket() throws Exception {
        UUID buyerId = UUID.randomUUID();
        storeMugAndTee(buyerId);
        expectCreateOrder(buyerId).andRespond(withStatus(HttpStatus.CREATED)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"id\":42,\"buyerId\":\"" + buyerId + "\",\"createdAt\":\"2026-11-24T10:00:00Z\","
                        + "\"total\":39.30,\"items\":[]}"));

        mockMvc.perform(post("/basket/{buyerId}/checkout", buyerId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(42));

        assertThat(redis.hasKey(BasketRepository.key(buyerId))).isFalse();
        mockMvc.perform(get("/basket/{buyerId}", buyerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void orderingServerErrorKeepsBasket() {
        UUID buyerId = UUID.randomUUID();
        String stored = storeMugAndTee(buyerId);
        expectCreateOrder(buyerId).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertCheckoutFailsAndKeepsBasket(buyerId, stored);
    }

    @Test
    void orderingRejectsKeepsBasket() {
        UUID buyerId = UUID.randomUUID();
        String stored = storeMugAndTee(buyerId);
        expectCreateOrder(buyerId).andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertCheckoutFailsAndKeepsBasket(buyerId, stored);
    }

    @Test
    void orderingUnreachableKeepsBasket() {
        UUID buyerId = UUID.randomUUID();
        String stored = storeMugAndTee(buyerId);
        expectCreateOrder(buyerId).andRespond(withException(new ConnectException("Connection refused")));

        assertCheckoutFailsAndKeepsBasket(buyerId, stored);
    }

    @Test
    void emptyBasketReturns400() throws Exception {
        UUID buyerId = UUID.randomUUID();
        redis.opsForValue().set(BasketRepository.key(buyerId), "{\"buyerId\":\"" + buyerId + "\",\"items\":[]}");

        mockMvc.perform(post("/basket/{buyerId}/checkout", buyerId))
                .andExpect(status().isBadRequest());

        assertThat(redis.hasKey(BasketRepository.key(buyerId))).isTrue();
    }

    @Test
    void missingBasketReturns400() throws Exception {
        UUID buyerId = UUID.randomUUID();

        mockMvc.perform(post("/basket/{buyerId}/checkout", buyerId))
                .andExpect(status().isBadRequest());

        assertThat(redis.hasKey(BasketRepository.key(buyerId))).isFalse();
    }
}
