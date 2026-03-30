package com.roger.redis.config;

import com.roger.redis.serializer.KryoRedisSerializer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.CacheKeyPrefix;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

/**
 * Redis configuration for caching and manual Redis operations.
 *
 * <h2>IMPORTANT: Dual Serialization Strategy</h2>
 * <p>This class intentionally uses <strong>two different serialization formats</strong>:</p>
 * <ol>
 *   <li><strong>{@link RedisCacheManager} &rarr; JSON</strong> ({@link GenericJackson2JsonRedisSerializer}) —
 *       Used by Spring's {@code @Cacheable} / {@code @CacheEvict} / {@code @CachePut} abstraction.
 *       JSON is required here because Spring Boot DevTools reloads classes with a different classloader,
 *       causing Kryo deserialization failures ({@code ClassCastException}) for cached DTOs. JSON survives
 *       classloader changes and is also human-readable in Redis for debugging.</li>
 *   <li><strong>{@link RedisTemplate} &rarr; Kryo</strong> ({@link KryoRedisSerializer}) —
 *       Used for manual Redis operations (geospatial commands, hash operations). These are not
 *       affected by DevTools classloader issues since they go through the template directly, and
 *       Kryo provides more compact binary representation for non-cache data.</li>
 * </ol>
 * <p><strong>Do not unify these into a single serializer</strong> without understanding the
 * DevTools classloader implications. Removing JSON from the cache manager will cause runtime
 * failures during development.</p>
 *
 * <h2>Other Design Decisions</h2>
 * <ul>
 *   <li><strong>StringRedisSerializer for keys</strong> — keeps keys human-readable in Redis.</li>
 *   <li><strong>Namespace prefix ({@link #CACHE_KEY_NAMESPACE})</strong> — isolates this application's
 *       cache entries from other applications sharing the same Redis instance.</li>
 *   <li><strong>Per-cache TTL overrides</strong> — different caches have different invalidation
 *       requirements based on how frequently their data changes.</li>
 * </ul>
 *
 * @see GenericJackson2JsonRedisSerializer
 * @see KryoRedisSerializer
 * @see RedisCacheManager
 * @see RedisTemplate
 */
@Configuration
public class RedisConfig {

    /**
     * Prefix for Spring Cache keys in Redis. Bumped when the value serialization format changes
     * (e.g. Kryo to JSON) so stale binary entries are not read.
     */
    public static final String CACHE_KEY_NAMESPACE = "restapi-json-v3::";

    /**
     * Configures and returns the {@link RedisCacheManager} used by Spring's caching abstraction.
     *
     * <p>The default cache configuration applies to all caches unless overridden:</p>
     * <ul>
     *   <li>Keys are serialized with {@link StringRedisSerializer} for readability.</li>
     *   <li>Values are serialized with {@link GenericJackson2JsonRedisSerializer} (JSON with default typing).</li>
     *   <li>Default TTL is 10 minutes to prevent stale data from accumulating.</li>
     *   <li>Null values are not cached to avoid storing meaningless entries.</li>
     *   <li>All keys are prefixed with {@link #CACHE_KEY_NAMESPACE} for namespace isolation.</li>
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
        RedisSerializer<Object> jsonValueSerializer = new GenericJackson2JsonRedisSerializer();

        var defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .computePrefixWith(CacheKeyPrefix.prefixed(CACHE_KEY_NAMESPACE))
                .disableCachingNullValues()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(jsonValueSerializer));

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
