package com.roger.redis.controller;

import com.roger.redis.service.CacheService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;

/**
 * REST controller for cache introspection and management.
 *
 * <p>Provides endpoints to inspect Redis cache keys, retrieve cache
 * statistics (including Redis {@code INFO memory} output), and evict
 * individual or all caches managed by the Spring {@link org.springframework.cache.CacheManager}.</p>
 *
 * <p>All key-listing operations use the Redis {@code SCAN} command
 * under the hood rather than {@code KEYS}, making them safe for
 * production use on large datasets. Eviction operations delegate
 * to {@link org.springframework.cache.Cache#clear()}, which removes
 * all entries belonging to the given cache prefix.</p>
 *
 * @see CacheService
 */
@RestController
@RequestMapping("/api/v1/cache")
public class CacheController {

    private final CacheService cacheService;

    /**
     * Constructs a new {@code CacheController}.
     *
     * @param cacheService the service providing cache introspection and management operations
     */
    public CacheController(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    /**
     * Lists all Redis cache keys grouped by cache name.
     *
     * <p>Iterates over every cache registered in the
     * {@link org.springframework.cache.CacheManager} and uses the Redis
     * {@code SCAN} command (with a match pattern per cache) to discover
     * the keys belonging to each cache.</p>
     *
     * @return a {@code 200 OK} response containing a map of cache names
     *         to their respective sets of Redis keys
     */
    @GetMapping("/keys")
    public ResponseEntity<Map<String, Set<String>>> getAllCacheKeys() {
        var keys = cacheService.getAllCacheKeys();
        return ResponseEntity.ok(keys);
    }

    /**
     * Lists all Redis keys belonging to a specific cache.
     *
     * <p>Uses the Redis {@code SCAN} command with a match pattern of
     * {@code restapi-redis::<cacheName>::*} to find all keys under the
     * given cache namespace.</p>
     *
     * @param cacheName the name of the cache whose keys should be listed
     * @return a {@code 200 OK} response containing the set of Redis keys
     *         for the specified cache
     */
    @GetMapping("/keys/{cacheName}")
    public ResponseEntity<Set<String>> getCacheKeysByName(@PathVariable String cacheName) {
        var keys = cacheService.getCacheKeysByName(cacheName);
        return ResponseEntity.ok(keys);
    }

    /**
     * Returns cache statistics including registered cache names, total key
     * count, per-cache key counts, and Redis memory information.
     *
     * <p>The statistics are assembled by scanning all cache keys via Redis
     * {@code SCAN} and retrieving memory metrics via the Redis
     * {@code INFO memory} command.</p>
     *
     * @return a {@code 200 OK} response containing a map of cache statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        var stats = cacheService.getCacheStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * Evicts (clears) all entries from the specified cache.
     *
     * <p>Delegates to {@link org.springframework.cache.Cache#clear()},
     * which internally issues Redis {@code DEL} commands for every key
     * matching the cache's key prefix.</p>
     *
     * @param cacheName the name of the cache to evict
     * @return a {@code 200 OK} response with a confirmation message
     */
    @DeleteMapping("/{cacheName}")
    public ResponseEntity<Map<String, String>> evictCache(@PathVariable String cacheName) {
        cacheService.evictCache(cacheName);
        return ResponseEntity.ok(Map.of("message", "Cache '" + cacheName + "' evicted successfully"));
    }

    /**
     * Evicts (clears) all entries from every registered cache.
     *
     * <p>Iterates over all cache names in the
     * {@link org.springframework.cache.CacheManager} and calls
     * {@link org.springframework.cache.Cache#clear()} on each,
     * issuing Redis {@code DEL} commands for all matching keys.</p>
     *
     * @return a {@code 200 OK} response with a confirmation message
     */
    @DeleteMapping
    public ResponseEntity<Map<String, String>> evictAllCaches() {
        cacheService.evictAllCaches();
        return ResponseEntity.ok(Map.of("message", "All caches evicted successfully"));
    }
}
