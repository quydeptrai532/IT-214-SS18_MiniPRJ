package com.rikkeibank.account.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis Cache-Aside cho tài khoản.
 *
 * - JSON serializer: dữ liệu trong Redis đọc được bằng redis-cli / Redis Insight.
 * - TTL: lưới an toàn cuối cùng nếu cache và DB lệch nhau.
 * - serializeKeysWith String: khóa dạng "shopmart:accounts::1" dễ nhìn.
 */
@Configuration
public class RedisCacheConfig {

    public static final String ACCOUNT_CACHE = "accounts";
    public static final String ACCOUNT_LIST_CACHE = "account-list";

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory,
                                     @Value("${rikkeibank.cache.account-ttl:5m}") Duration ttl) {
        RedisCacheConfiguration configuration = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .disableCachingNullValues()
                .prefixCacheNameWith("rikkeibank:")
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(configuration)
                .withCacheConfiguration(ACCOUNT_CACHE, configuration)
                .withCacheConfiguration(ACCOUNT_LIST_CACHE, configuration)
                .build();
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }
}
