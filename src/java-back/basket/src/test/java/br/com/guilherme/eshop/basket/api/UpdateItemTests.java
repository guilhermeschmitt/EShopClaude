package br.com.guilherme.eshop.basket.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import br.com.guilherme.eshop.basket.TestcontainersConfiguration;
import br.com.guilherme.eshop.basket.repository.BasketRepository;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureMockRestServiceServer
@Import(TestcontainersConfiguration.class)
class UpdateItemTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MockRestServiceServer catalog;

    @Autowired
    private StringRedisTemplate redis;

    @BeforeEach
    void reset() {
        catalog.reset();
        Set<String> keys = redis.keys("basket:*");
        if (!keys.isEmpty()) {
            redis.delete(keys);
        }
    }

    /** No test sets an expectation, so this proves an update never calls Catalog. */
    @AfterEach
    void catalogNeverCalled() {
        catalog.verify();
    }

    private String storeMugAndTee(UUID buyerId) {
        String json = "{\"buyerId\":\"" + buyerId + "\",\"items\":["
                + "{\"productId\":1,\"productName\":\"Mug\",\"unitPrice\":9.90,\"quantity\":2},"
                + "{\"productId\":2,\"productName\":\"Tee\",\"unitPrice\":19.50,\"quantity\":1}]}";
        redis.opsForValue().set(BasketRepository.key(buyerId), json);
        return json;
    }

    private ResultActions putQuantity(Object buyerId, long productId, String body) throws Exception {
        return mockMvc.perform(put("/basket/{buyerId}/items/{productId}", buyerId, productId)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void updateSetsQuantityAndTotal() throws Exception {
        UUID buyerId = UUID.randomUUID();
        storeMugAndTee(buyerId);

        putQuantity(buyerId, 1, "{\"quantity\":5}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].productId").value(1))
                .andExpect(jsonPath("$.items[0].productName").value("Mug"))
                .andExpect(jsonPath("$.items[0].unitPrice").value(9.90))
                .andExpect(jsonPath("$.items[0].quantity").value(5))
                .andExpect(jsonPath("$.items[1].quantity").value(1))
                .andExpect(jsonPath("$.total").value(69.00));

        mockMvc.perform(get("/basket/{buyerId}", buyerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(5))
                .andExpect(jsonPath("$.total").value(69.00));
    }

    @Test
    void zeroRemovesLine() throws Exception {
        UUID buyerId = UUID.randomUUID();
        storeMugAndTee(buyerId);

        putQuantity(buyerId, 1, "{\"quantity\":0}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].productId").value(2))
                .andExpect(jsonPath("$.total").value(19.50));

        putQuantity(buyerId, 2, "{\"quantity\":0}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.total").value(0));

        assertThat(redis.hasKey(BasketRepository.key(buyerId))).isTrue();
    }

    @Test
    void unknownLineReturns404() throws Exception {
        UUID buyerId = UUID.randomUUID();
        String stored = storeMugAndTee(buyerId);

        putQuantity(buyerId, 99, "{\"quantity\":1}").andExpect(status().isNotFound());

        assertThat(redis.opsForValue().get(BasketRepository.key(buyerId))).isEqualTo(stored);
    }

    @Test
    void missingBasketReturns404() throws Exception {
        UUID buyerId = UUID.randomUUID();

        putQuantity(buyerId, 1, "{\"quantity\":1}").andExpect(status().isNotFound());

        assertThat(redis.hasKey(BasketRepository.key(buyerId))).isFalse();
    }

    @Test
    void negativeQuantityReturns400() throws Exception {
        UUID buyerId = UUID.randomUUID();
        String stored = storeMugAndTee(buyerId);

        putQuantity(buyerId, 1, "{\"quantity\":-1}").andExpect(status().isBadRequest());

        assertThat(redis.opsForValue().get(BasketRepository.key(buyerId))).isEqualTo(stored);
    }

    @Test
    void missingQuantityReturns400() throws Exception {
        UUID buyerId = UUID.randomUUID();
        storeMugAndTee(buyerId);

        putQuantity(buyerId, 1, "{}").andExpect(status().isBadRequest());
    }
}
