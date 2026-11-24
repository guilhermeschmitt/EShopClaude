package br.com.guilherme.eshop.basket.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.web.servlet.MockMvc;

import br.com.guilherme.eshop.basket.TestcontainersConfiguration;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class BasketControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StringRedisTemplate redis;

    @BeforeEach
    void clearBaskets() {
        Set<String> keys = redis.keys("basket:*");
        if (!keys.isEmpty()) {
            redis.delete(keys);
        }
    }

    @Test
    void missingBasketReturnsEmpty() throws Exception {
        UUID buyerId = UUID.randomUUID();

        mockMvc.perform(get("/basket/{buyerId}", buyerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.buyerId").value(buyerId.toString()))
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.total").value(0.0));
    }

    @Test
    void storedBasketReturnsLinesAndTotal() throws Exception {
        UUID buyerId = UUID.randomUUID();
        String json = "{\"buyerId\":\"" + buyerId + "\",\"items\":["
                + "{\"productId\":1,\"productName\":\"Mug\",\"unitPrice\":9.90,\"quantity\":2},"
                + "{\"productId\":2,\"productName\":\"Tee\",\"unitPrice\":19.50,\"quantity\":1}]}";
        redis.opsForValue().set("basket:" + buyerId, json);

        mockMvc.perform(get("/basket/{buyerId}", buyerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.buyerId").value(buyerId.toString()))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].productId").value(1))
                .andExpect(jsonPath("$.items[0].productName").value("Mug"))
                .andExpect(jsonPath("$.items[0].unitPrice").value(9.90))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[1].productId").value(2))
                .andExpect(jsonPath("$.items[1].productName").value("Tee"))
                .andExpect(jsonPath("$.items[1].unitPrice").value(19.50))
                .andExpect(jsonPath("$.items[1].quantity").value(1))
                .andExpect(jsonPath("$.total").value(39.30));
    }

    @Test
    void invalidBuyerIdReturns400() throws Exception {
        mockMvc.perform(get("/basket/not-a-uuid"))
                .andExpect(status().isBadRequest());
    }
}
