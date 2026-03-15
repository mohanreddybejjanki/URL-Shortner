package com.project.urlshortener.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis Cache Configuration.
 *
 * Why Redis for caching?
 * → In-memory storage: microsecond read speed vs milliseconds for MySQL.
 * → Reduces DB load under high traffic.
 * → TTL support: entries auto-expire without manual cleanup.
 *
 * How caching works in this app:
 * 1. User visits /aB3xY (first time)
 * 2. resolveUrl() has @Cacheable("urls") → cache MISS → queries MySQL
 * 3. Result stored in Redis: key="urls::aB3xY", value="https://original-url.com"
 * 4. User visits /aB3xY again → cache HIT → reads from Redis, skips MySQL
 * 5. After TTL (1 hour), entry expires → next request goes to MySQL again
 *
 * Lettuce vs Jedis:
 * → Lettuce is non-blocking (Netty-based) and is Spring Boot's default.
 * → Jedis is blocking but simpler. Either works for this project.
 */
@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    /**
     * Connection factory using Lettuce (non-blocking Netty client).
     * RedisStandaloneConfiguration → single Redis node (not cluster).
     */
    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config =
                new RedisStandaloneConfiguration(redisHost, redisPort);
        return new LettuceConnectionFactory(config);
    }

    /**
     * RedisTemplate for direct Redis operations (optional, used if you need
     * manual get/set beyond Spring's @Cacheable abstraction).
     *
     * StringRedisSerializer → keys stored as plain strings (readable in Redis CLI)
     * GenericJackson2JsonRedisSerializer → values stored as JSON (not Java-serialized bytes)
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    /**
     * CacheManager backed by Redis.
     *
     * RedisCacheConfiguration.defaultCacheConfig()
     *   .entryTtl(Duration.ofHours(1))  → cache entries expire after 1 hour
     *   .disableCachingNullValues()      → don't cache null results
     *   .serializeValuesWith(...)        → store values as JSON strings
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration cacheConfig = RedisCacheConfiguration
                .defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .disableCachingNullValues()
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new GenericJackson2JsonRedisSerializer()
                        )
                );

        return RedisCacheManager.builder(factory)
                .cacheDefaults(cacheConfig)
                .build();
    }
}