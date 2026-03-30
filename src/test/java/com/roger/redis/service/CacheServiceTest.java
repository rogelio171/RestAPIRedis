package com.roger.redis.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CacheService}.
 *
 * @author Roger
 */
@ExtendWith(MockitoExtension.class)
class CacheServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private CacheManager cacheManager;

    @InjectMocks
    private CacheService cacheService;

    @Test
    @DisplayName("getAllCacheKeys — should return keys grouped by cache name")
    void getAllCacheKeys_shouldReturnKeysGroupedByCacheName() {
        when(cacheManager.getCacheNames()).thenReturn(List.of("countries", "countries:byRegion"));
        when(redisTemplate.execute(any(RedisCallback.class))).thenReturn(Set.of("restapi-json::countries::key1"));

        var result = cacheService.getAllCacheKeys();

        assertThat(result).containsKeys("countries", "countries:byRegion");
        assertThat(result.get("countries")).isNotNull();
        verify(cacheManager).getCacheNames();
    }

    @Test
    @DisplayName("getCacheKeysByName — should return set of keys for cache")
    void getCacheKeysByName_shouldReturnSetOfKeys() {
        var keys = Set.of("restapi-json::countries::key1", "restapi-json::countries::key2");
        when(redisTemplate.execute(any(RedisCallback.class))).thenReturn(keys);

        var result = cacheService.getCacheKeysByName("countries");

        assertThat(result).hasSize(2).containsAll(keys);
    }

    @Test
    @DisplayName("getCacheKeysByName — should return empty set when no keys")
    void getCacheKeysByName_shouldReturnEmptySetWhenNoKeys() {
        when(redisTemplate.execute(any(RedisCallback.class))).thenReturn(Set.of());

        var result = cacheService.getCacheKeysByName("countries");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getCacheStats — should return cacheNames, totalKeys, keysByCache, redisInfo")
    void getCacheStats_shouldReturnStatsMap() {
        when(cacheManager.getCacheNames()).thenReturn(List.of("countries"));
        when(redisTemplate.execute(any(RedisCallback.class))).thenReturn(Set.of("restapi-json::countries::k1"));
        when(redisTemplate.getConnectionFactory()).thenReturn(null);

        var result = cacheService.getCacheStats();

        assertThat(result).containsKeys("cacheNames", "totalKeys", "keysByCache", "redisInfo");
        assertThat(result.get("cacheNames")).asList().containsExactly("countries");
        assertThat(result.get("totalKeys")).isEqualTo(1);
        assertThat(result.get("redisInfo")).isNull();
    }

    @Test
    @DisplayName("evictCache — should clear cache when cache exists")
    void evictCache_shouldClearCacheWhenExists() {
        Cache cache = org.mockito.Mockito.mock(Cache.class);
        when(cacheManager.getCache("countries")).thenReturn(cache);

        cacheService.evictCache("countries");

        verify(cache).clear();
    }

    @Test
    @DisplayName("evictCache — should do nothing when cache does not exist")
    void evictCache_shouldDoNothingWhenCacheNull() {
        when(cacheManager.getCache("nonexistent")).thenReturn(null);

        cacheService.evictCache("nonexistent");

        verify(cacheManager).getCache("nonexistent");
    }

    @Test
    @DisplayName("evictAllCaches — should clear all caches")
    void evictAllCaches_shouldClearAllCaches() {
        Cache cache1 = org.mockito.Mockito.mock(Cache.class);
        Cache cache2 = org.mockito.Mockito.mock(Cache.class);
        when(cacheManager.getCacheNames()).thenReturn(List.of("c1", "c2"));
        when(cacheManager.getCache("c1")).thenReturn(cache1);
        when(cacheManager.getCache("c2")).thenReturn(cache2);

        cacheService.evictAllCaches();

        verify(cache1).clear();
        verify(cache2).clear();
    }
}
