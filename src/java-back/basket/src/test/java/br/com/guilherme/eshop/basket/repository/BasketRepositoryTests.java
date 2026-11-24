package br.com.guilherme.eshop.basket.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;

import br.com.guilherme.eshop.basket.TestcontainersConfiguration;
import br.com.guilherme.eshop.basket.domain.Basket;
import br.com.guilherme.eshop.basket.domain.BasketItem;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class BasketRepositoryTests {

    @Autowired
    private BasketRepository repository;

    @Autowired
    private StringRedisTemplate redis;

    @Autowired
    private JsonMapper jsonMapper;

    @BeforeEach
    void clearBaskets() {
        Set<String> keys = redis.keys("basket:*");
        if (!keys.isEmpty()) {
            redis.delete(keys);
        }
    }

    @Test
    void findMissingReturnsEmpty() {
        assertThat(repository.find(UUID.randomUUID())).isEmpty();
    }

    @Test
    void saveThenFindRoundTrips() {
        UUID buyerId = UUID.randomUUID();
        repository.save(sampleBasket(buyerId));

        Basket found = repository.find(buyerId).orElseThrow();

        assertThat(found.buyerId()).isEqualTo(buyerId);
        assertThat(found.items()).hasSize(2);
        assertThat(found.items().get(0).productId()).isEqualTo(1);
        assertThat(found.items().get(0).productName()).isEqualTo("Mug");
        assertThat(found.items().get(0).unitPrice()).isEqualByComparingTo("9.90");
        assertThat(found.items().get(0).quantity()).isEqualTo(2);
        assertThat(found.items().get(1).productId()).isEqualTo(2);
        assertThat(found.items().get(1).productName()).isEqualTo("Tee");
        assertThat(found.items().get(1).unitPrice()).isEqualByComparingTo("19.50");
        assertThat(found.items().get(1).quantity()).isEqualTo(1);
    }

    @Test
    void storesJsonWithoutTotalOrTtl() {
        UUID buyerId = UUID.randomUUID();
        repository.save(sampleBasket(buyerId));
        String key = BasketRepository.key(buyerId);

        JsonNode stored = jsonMapper.readTree(redis.opsForValue().get(key));

        assertThat(stored.propertyNames()).containsExactlyInAnyOrder("buyerId", "items");
        assertThat(stored.get("buyerId").asString()).isEqualTo(buyerId.toString());
        assertThat(redis.getExpire(key)).isEqualTo(-1L);
    }

    private static Basket sampleBasket(UUID buyerId) {
        return new Basket(buyerId, List.of(
                new BasketItem(1, "Mug", new BigDecimal("9.90"), 2),
                new BasketItem(2, "Tee", new BigDecimal("19.50"), 1)));
    }
}
