package com.roger.redis.service;

import com.roger.redis.config.RedisConfig;

import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service providing cache introspection and management capabilities.
 *
 * <p>This service acts as a bridge between Spring's cache abstraction
 * ({@link CacheManager}) and the underlying Redis instance
 * ({@link RedisTemplate}), exposing operations to list keys, gather
 * statistics, and evict caches programmatically.</p>
 *
 * <h2>Why SCAN instead of KEYS?</h2>
 * <p>The Redis {@code KEYS} command iterates over the <em>entire</em> keyspace
 * in a single blocking call. In production, where Redis may hold millions of
 * keys, this blocks the server for a potentially long time, causing latency
 * spikes for every other client. The {@code SCAN} command, on the other hand,
 * returns results incrementally via a cursor, spreading the work across
 * multiple short iterations and allowing the server to continue serving other
 * requests between iterations. This makes {@code SCAN} safe for production
 * use even on large datasets.</p>
 *
 * @see CacheManager
 * @see RedisTemplate
 * @see ScanOptions
 */
@Service
public class CacheService {

    private static final Logger log = LoggerFactory.getLogger(CacheService.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final CacheManager cacheManager;

    /**
     * Constructs a new {@code CacheService}.
     *
     * @param redisTemplate the Redis template for direct Redis operations (SCAN, INFO)
     * @param cacheManager  the Spring cache manager for cache name resolution and eviction
     */
    public CacheService(RedisTemplate<String, Object> redisTemplate,
                        CacheManager cacheManager) {
        this.redisTemplate = redisTemplate;
        this.cacheManager = cacheManager;
    }

    /**
     * Returns all cache keys grouped by cache name.
     *
     * <p>Iterates over every cache name registered in the {@link CacheManager}
     * and uses the Redis {@code SCAN} command to discover matching keys.
     * The {@code SCAN} command is used instead of {@code KEYS} because
     * {@code KEYS} blocks Redis for the duration of the full keyspace
     * traversal, which is unacceptable in production environments with
     * large datasets.</p>
     *
     * @return a map where each key is a cache name and each value is the set
     *         of Redis keys belonging to that cache
     */
    public Map<String, Set<String>> getAllCacheKeys() {
        var result = new LinkedHashMap<String, Set<String>>();
        for (var cacheName : cacheManager.getCacheNames()) {
            result.put(cacheName, getCacheKeysByName(cacheName));
        }
        return result;
    }

    /**
     * Returns all Redis keys belonging to the specified cache.
     *
     * <p>Uses the Redis {@code SCAN} command with a match pattern of
     * {@code restapi-json::<cacheName>::*} and a count hint of 100.
     * The count hint is <em>not</em> a hard limit — Redis may return
     * more or fewer keys per iteration — but it guides the server on
     * how much work to do per call.</p>
     *
     * @param cacheName the name of the cache to scan for keys
     * @return the set of Redis keys for the given cache, or an empty set
     *         if no keys are found
     */
    public Set<String> getCacheKeysByName(String cacheName) {
        var pattern = RedisConfig.CACHE_KEY_NAMESPACE + cacheName + "::*";
        var scanOptions = ScanOptions.scanOptions()
                .match(pattern)
                .count(100)
                .build();

        var keys = redisTemplate.execute((RedisCallback<Set<String>>) connection -> {
            var foundKeys = new HashSet<String>();
            try (Cursor<byte[]> cursor = connection.keyCommands().scan(scanOptions)) {
                while (cursor.hasNext()) {
                    foundKeys.add(new String(cursor.next(), StandardCharsets.UTF_8));
                }
            }
            return foundKeys;
        });

        return keys != null ? keys : Set.of();
    }

    /**
     * Returns cache statistics including cache names, total key count,
     * per-cache key counts, and Redis memory information.
     *
     * <p>The {@code redisInfo} entry contains the output of the Redis
     * {@code INFO memory} command, which includes metrics such as
     * {@code used_memory}, {@code used_memory_human},
     * {@code used_memory_peak}, and more.</p>
     *
     * @return a map containing cache statistics and Redis memory info
     */
    public Map<String, Object> getCacheStats() {
        var stats = new LinkedHashMap<String, Object>();

        var cacheNames = cacheManager.getCacheNames();
        stats.put("cacheNames", cacheNames);

        var allKeys = getAllCacheKeys();
        var totalKeys = allKeys.values().stream()
                .mapToInt(Set::size)
                .sum();
        stats.put("totalKeys", totalKeys);

        var keysByCache = new LinkedHashMap<String, Integer>();
        allKeys.forEach((name, keys) -> keysByCache.put(name, keys.size()));
        stats.put("keysByCache", keysByCache);

        stats.put("redisInfo", getRedisMemoryInfo());

        return stats;
    }

    /**
     * Evicts (clears) all entries from the specified cache.
     *
     * <p>If the cache name does not correspond to a registered cache,
     * this method does nothing.</p>
     *
     * @param cacheName the name of the cache to evict
     */
    public void evictCache(String cacheName) {
        var cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
            log.info("Evicted cache: {}", cacheName);
        }
    }

    /**
     * Evicts (clears) all entries from every registered cache.
     */
    public void evictAllCaches() {
        for (var cacheName : cacheManager.getCacheNames()) {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        }
        log.info("Evicted all caches");
    }

    /**
     * Retrieves Redis memory information via the {@code INFO memory} command.
     *
     * @return a {@link Properties} object with memory metrics, or {@code null}
     *         if the connection factory is unavailable
     */
    private Properties getRedisMemoryInfo() {
        var connectionFactory = redisTemplate.getConnectionFactory();
        if (connectionFactory == null) {
            return null;
        }
        try (RedisConnection connection = connectionFactory.getConnection()) {
            return connection.serverCommands().info("memory");
        }
    }
}
