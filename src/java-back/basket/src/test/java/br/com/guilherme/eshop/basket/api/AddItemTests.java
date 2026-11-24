package br.com.guilherme.eshop.basket.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;
import java.util.UUID;

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
import org.springframework.test.web.servlet.MockMvc;

import br.com.guilherme.eshop.basket.TestcontainersConfiguration;
import br.com.guilherme.eshop.basket.repository.BasketRepository;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureMockRestServiceServer
@Import(TestcontainersConfiguration.class)
class AddItemTests {

    private static final String TSHIRT = """
            {"id":1,"name":"Classic Logo T-Shirt","description":"Soft cotton tee.","price":19.90,"category":"Apparel"}""";

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

    private void catalogReturnsTshirt() {
        catalog.expect(requestTo("http://catalog.test/products/1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(TSHIRT, MediaType.APPLICATION_JSON));
    }

    @Test
    void addNewProductSnapshotsCatalogData() throws Exception {
        UUID buyerId = UUID.randomUUID();
        catalogReturnsTshirt();

        mockMvc.perform(post("/basket/{buyerId}/items", buyerId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"productId\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.buyerId").value(buyerId.toString()))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].productId").value(1))
                .andExpect(jsonPath("$.items[0].productName").value("Classic Logo T-Shirt"))
                .andExpect(jsonPath("$.items[0].unitPrice").value(19.90))
                .andExpect(jsonPath("$.items[0].quantity").value(1))
                .andExpect(jsonPath("$.total").value(19.90));

        assertThat(redis.hasKey(BasketRepository.key(buyerId))).isTrue();
        catalog.verify();
    }

    @Test
    void addExistingProductIncrementsAndRefreshesSnapshot() throws Exception {
        UUID buyerId = UUID.randomUUID();
        redis.opsForValue().set(BasketRepository.key(buyerId), "{\"buyerId\":\"" + buyerId + "\",\"items\":["
                + "{\"productId\":1,\"productName\":\"Old name\",\"unitPrice\":5.00,\"quantity\":1}]}");
        catalogReturnsTshirt();

        mockMvc.perform(post("/basket/{buyerId}/items", buyerId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"productId\":1,\"quantity\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].productName").value("Classic Logo T-Shirt"))
                .andExpect(jsonPath("$.items[0].unitPrice").value(19.90))
                .andExpect(jsonPath("$.items[0].quantity").value(3))
                .andExpect(jsonPath("$.total").value(59.70));

        catalog.verify();
    }

    @Test
    void unknownProductReturns404() throws Exception {
        UUID buyerId = UUID.randomUUID();
        catalog.expect(requestTo("http://catalog.test/products/99999"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        mockMvc.perform(post("/basket/{buyerId}/items", buyerId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"productId\":99999}"))
                .andExpect(status().isNotFound());

        assertThat(redis.hasKey(BasketRepository.key(buyerId))).isFalse();
        catalog.verify();
    }

    @Test
    void quantityBelowOneReturns400() throws Exception {
        mockMvc.perform(post("/basket/{buyerId}/items", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"productId\":1,\"quantity\":0}"))
                .andExpect(status().isBadRequest());

        catalog.verify();
    }

    @Test
    void missingProductIdReturns400() throws Exception {
        mockMvc.perform(post("/basket/{buyerId}/items", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":1}"))
                .andExpect(status().isBadRequest());

        catalog.verify();
    }

    @Test
    void getDoesNotCallCatalog() throws Exception {
        UUID buyerId = UUID.randomUUID();
        redis.opsForValue().set(BasketRepository.key(buyerId), "{\"buyerId\":\"" + buyerId + "\",\"items\":["
                + "{\"productId\":1,\"productName\":\"Mug\",\"unitPrice\":9.90,\"quantity\":2}]}");

        mockMvc.perform(get("/basket/{buyerId}", buyerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(19.80));

        catalog.verify();
    }
}
