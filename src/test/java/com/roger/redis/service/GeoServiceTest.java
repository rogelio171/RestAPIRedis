package com.roger.redis.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.domain.geo.Metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link GeoService}.
 *
 * <p>Uses Mockito to mock the {@link RedisTemplate} and its derived operations
 * ({@link GeoOperations}, {@link HashOperations}, {@link ZSetOperations}) so
 * that tests verify the service-layer delegation logic without requiring a
 * running Redis instance.</p>
 *
 * @author Roger
 * @see GeoService
 */
@ExtendWith(MockitoExtension.class)
class GeoServiceTest {

    @Mock
    RedisTemplate<String, Object> redisTemplate;

    @Mock
    GeoOperations<String, Object> geoOperations;

    @Mock
    HashOperations<String, Object, Object> hashOperations;

    @Mock
    ZSetOperations<String, Object> zSetOperations;

    @InjectMocks
    GeoService geoService;

    // ──────────────────────────────────────────────
    // Tests
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("addCountryLocation — should call GEOADD and HSET on Redis")
    void addCountryLocation_shouldCallGeoAddAndHashPut() {
        // Arrange
        when(redisTemplate.opsForGeo()).thenReturn(geoOperations);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);

        var countryCode = "USA";
        var countryName = "United States";
        var longitude = -98.5795;
        var latitude = 39.8283;

        // Act
        geoService.addCountryLocation(countryCode, countryName, longitude, latitude);

        // Assert
        verify(geoOperations).add(eq("geo:countries"), any(Point.class), eq("USA"));
        verify(hashOperations).put("geo:names", "USA", "United States");
    }

    @Test
    @DisplayName("getDistanceBetween — should return distance in kilometres")
    void getDistanceBetween_shouldReturnDistance() {
        // Arrange
        when(redisTemplate.opsForGeo()).thenReturn(geoOperations);

        var distance = new Distance(1_957.0, Metrics.KILOMETERS);
        when(geoOperations.distance("geo:countries", "USA", "MEX", Metrics.KILOMETERS))
                .thenReturn(distance);

        // Act
        var result = geoService.getDistanceBetween("USA", "MEX");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(1_957.0);
        verify(geoOperations).distance("geo:countries", "USA", "MEX", Metrics.KILOMETERS);
    }

    @Test
    @DisplayName("getGeoIndexSize — should return the number of entries via ZCARD")
    void getGeoIndexSize_shouldReturnSize() {
        // Arrange
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.size("geo:countries")).thenReturn(250L);

        // Act
        var result = geoService.getGeoIndexSize();

        // Assert
        assertThat(result).isEqualTo(250L);
        verify(zSetOperations).size("geo:countries");
    }
}
