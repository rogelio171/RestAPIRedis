package com.roger.redis.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.connection.RedisGeoCommands.GeoRadiusCommandArgs;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.domain.geo.Metrics;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
    @DisplayName("addCountryLocation — should skip coordinates invalid for Redis GEO")
    void addCountryLocation_shouldSkipInvalidCoordinates() {
        geoService.addCountryLocation("ATA", "Antarctica", 0.0, -90.0);

        verify(redisTemplate, never()).opsForGeo();
        verify(redisTemplate, never()).opsForHash();
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
    @DisplayName("findNearbyCountries — should batch-fetch names via multiGet instead of individual gets")
    @SuppressWarnings("unchecked")
    void findNearbyCountries_shouldUseBatchMultiGet() {
        when(redisTemplate.opsForGeo()).thenReturn(geoOperations);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);

        var loc1 = new RedisGeoCommands.GeoLocation<Object>("USA", new Point(-97.0, 38.0));
        var loc2 = new RedisGeoCommands.GeoLocation<Object>("MEX", new Point(-102.0, 23.0));

        var result1 = new GeoResult<>(loc1, new Distance(0.0, Metrics.KILOMETERS));
        var result2 = new GeoResult<>(loc2, new Distance(1957.0, Metrics.KILOMETERS));

        var geoResults = new GeoResults<>(List.of(result1, result2));

        when(geoOperations.radius(eq("geo:countries"), any(Circle.class), any(GeoRadiusCommandArgs.class)))
                .thenReturn(geoResults);
        when(hashOperations.multiGet(eq("geo:names"), anyList()))
                .thenReturn(List.of("United States", "Mexico"));

        var results = geoService.findNearbyCountries(38.0, -97.0, 3000.0, 10);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).countryCode()).isEqualTo("USA");
        assertThat(results.get(0).countryName()).isEqualTo("United States");
        assertThat(results.get(1).countryCode()).isEqualTo("MEX");
        assertThat(results.get(1).countryName()).isEqualTo("Mexico");

        verify(hashOperations).multiGet(eq("geo:names"), anyList());
        verify(hashOperations, never()).get(any(), any());
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
