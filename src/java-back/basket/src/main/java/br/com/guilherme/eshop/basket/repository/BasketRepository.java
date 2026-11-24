package br.com.guilherme.eshop.basket.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import br.com.guilherme.eshop.basket.domain.Basket;
import tools.jackson.databind.json.JsonMapper;

/** Stores each basket as a JSON string under basket:{buyerId}, with no TTL. */
@Repository
public class BasketRepository {

    private final StringRedisTemplate redis;
    private final JsonMapper jsonMapper;

    public BasketRepository(StringRedisTemplate redis, JsonMapper jsonMapper) {
        this.redis = redis;
        this.jsonMapper = jsonMapper;
    }

    public static String key(UUID buyerId) {
        return "basket:" + buyerId;
    }

    public Optional<Basket> find(UUID buyerId) {
        String json = redis.opsForValue().get(key(buyerId));
        return json == null ? Optional.empty() : Optional.of(jsonMapper.readValue(json, Basket.class));
    }

    public void save(Basket basket) {
        redis.opsForValue().set(key(basket.buyerId()), jsonMapper.writeValueAsString(basket));
    }
}
