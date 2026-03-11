package com.roger.redis.config;

import com.roger.redis.serializer.KryoRedisSerializer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.CacheKeyPrefix;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

/**
 * Redis configuration for caching and manual Redis operations.
 *
 * <p>This configuration class sets up two primary components:</p>
 * <ol>
 *   <li>A {@link RedisCacheManager} for Spring's {@code @Cacheable} / {@code @CacheEvict} / {@code @CachePut}
 *       abstraction, using our custom {@link KryoRedisSerializer} for compact, high-performance binary
 *       serialization of cache values.</li>
 *   <li>A {@link RedisTemplate} for manual Redis operations (e.g., geospatial commands, pub/sub, or any
 *       use case not covered by the cache abstraction).</li>
 * </ol>
 *
 * <h2>Key Design Decisions</h2>
 * <ul>
 *   <li><strong>Kryo for values</strong> — provides significantly smaller payloads and faster
 *       serialization/deserialization compared to JSON or JDK serialization.</li>
 *   <li><strong>StringRedisSerializer for keys</strong> — keeps keys human-readable in Redis,
 *       simplifying debugging and monitoring.</li>
 *   <li><strong>Namespace prefix ({@code restapi-redis::})</strong> — isolates this application's
 *       cache entries from other applications sharing the same Redis instance.</li>
 *   <li><strong>Per-cache TTL overrides</strong> — different caches have different invalidation
 *       requirements based on how frequently their data changes.</li>
 * </ul>
 *
 * @see KryoRedisSerializer
 * @see RedisCacheManager
 * @see RedisTemplate
 */
@Configuration
public class RedisConfig {

    /**
     * Configures and returns the {@link RedisCacheManager} used by Spring's caching abstraction.
     *
     * <p>The default cache configuration applies to all caches unless overridden:</p>
     * <ul>
     *   <li>Keys are serialized with {@link StringRedisSerializer} for readability.</li>
     *   <li>Values are serialized with {@link KryoRedisSerializer} for performance and compactness.</li>
     *   <li>Default TTL is 10 minutes to prevent stale data from accumulating.</li>
     *   <li>Null values are not cached to avoid storing meaningless entries.</li>
     *   <li>All keys are prefixed with {@code restapi-redis::} for namespace isolation.</li>
     * </ul>
     *
     * <p>Per-cache TTL overrides are applied for domain-specific caches:</p>
     * <ul>
     *   <li>{@code "countries"} — 30 minutes (country list changes infrequently).</li>
     *   <li>{@code "countries:byRegion"} — 20 minutes (regional groupings are relatively stable).</li>
     *   <li>{@code "countries:search"} — 5 minutes (search results may change more frequently).</li>
     * </ul>
     *
     * @param connectionFactory the Redis connection factory provided by Spring Boot auto-configuration
     * @return a fully configured {@link RedisCacheManager}
     */
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        var kryoSerializer = new KryoRedisSerializer();

        var defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .computePrefixWith(CacheKeyPrefix.prefixed("restapi-redis::"))
                .disableCachingNullValues()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(kryoSerializer));

        var perCacheConfigs = Map.of(
                "countries", defaultConfig.entryTtl(Duration.ofMinutes(30)),
                "countries:byRegion", defaultConfig.entryTtl(Duration.ofMinutes(20)),
                "countries:search", defaultConfig.entryTtl(Duration.ofMinutes(5))
        );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(perCacheConfigs)
                .build();
    }

    /**
     * Configures a {@link RedisTemplate} for manual Redis operations beyond the cache abstraction.
     *
     * <p>This template is useful for operations such as:</p>
     * <ul>
     *   <li>Geospatial commands ({@code GEOADD}, {@code GEOSEARCH})</li>
     *   <li>Pub/Sub messaging</li>
     *   <li>Direct key/value manipulation with expiration control</li>
     *   <li>Hash operations for structured data</li>
     * </ul>
     *
     * <p>The serialization strategy mirrors the cache manager configuration:</p>
     * <ul>
     *   <li>Keys and hash keys use {@link StringRedisSerializer} for readability.</li>
     *   <li>Values and hash values use {@link KryoRedisSerializer} for performance.</li>
     * </ul>
     *
     * @param connectionFactory the Redis connection factory provided by Spring Boot auto-configuration
     * @return a fully configured {@link RedisTemplate} for {@code String} keys and {@code Object} values
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        var template = new RedisTemplate<String, Object>();
        var kryoSerializer = new KryoRedisSerializer();

        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(kryoSerializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(kryoSerializer);

        return template;
    }
}
