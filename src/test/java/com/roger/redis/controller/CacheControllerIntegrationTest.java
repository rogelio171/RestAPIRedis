package com.roger.redis.controller;

import com.roger.redis.config.TestContainersConfig;
import com.roger.redis.config.TestSecurityConfig;
import com.roger.redis.model.dto.CountryDTO;
import com.roger.redis.model.entity.Country;
import com.roger.redis.repository.CountryRepository;
import com.roger.redis.seeder.DataSeeder;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link CacheController}.
 *
 * <p>Uses Testcontainers (PostgreSQL + Redis) to run against real infrastructure.
 * Test countries are inserted into the database and the cache is warmed by issuing
 * a GET request to {@code /api/v1/countries} before each test.</p>
 *
 * @author Roger
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "app.security.mode=disabled")
@Import({TestContainersConfig.class, TestSecurityConfig.class})
@AutoConfigureTestRestTemplate
class CacheControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CountryRepository countryRepository;

    /** Prevents the {@link DataSeeder} from calling the external REST Countries API. */
    @MockitoBean
    private DataSeeder dataSeeder;

    @BeforeEach
    void setUp() {
        seedTestCountries();
        warmCache();
    }

    @AfterEach
    void tearDown() {
        // Evict all caches so tests are isolated
        restTemplate.delete("/api/v1/cache");
        countryRepository.deleteAll();
    }

    // ──────────────────────────────────────────────
    // Tests
    // ──────────────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void getAllCacheKeys_shouldReturnKeys() {
        // The cache was warmed in setUp — query the cache keys endpoint
        var response = restTemplate.exchange(
                "/api/v1/cache/keys",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Set<String>>>() {}
        );

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isNotEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void getCacheStats_shouldReturnStats() {
        var response = restTemplate.getForEntity("/api/v1/cache/stats", Map.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsKey("cacheNames");
        assertThat(response.getBody()).containsKey("totalKeys");
    }

    @Test
    @SuppressWarnings("unchecked")
    void evictAllCaches_shouldClearAllKeys() {
        // Verify cache has entries before eviction
        var keysBefore = restTemplate.exchange(
                "/api/v1/cache/keys",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Set<String>>>() {}
        );
        assertThat(keysBefore.getBody()).isNotNull();

        // Evict all caches
        var deleteResponse = restTemplate.exchange(
                "/api/v1/cache",
                HttpMethod.DELETE,
                null,
                new ParameterizedTypeReference<Map<String, String>>() {}
        );
        assertThat(deleteResponse.getStatusCode().value()).isEqualTo(200);

        // Verify caches are now empty
        var keysAfter = restTemplate.exchange(
                "/api/v1/cache/keys",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Set<String>>>() {}
        );
        assertThat(keysAfter.getStatusCode().value()).isEqualTo(200);
        assertThat(keysAfter.getBody()).isNotNull();

        // All cache name entries should have empty sets after eviction
        var totalKeysAfter = keysAfter.getBody().values().stream()
                .mapToInt(Set::size)
                .sum();
        assertThat(totalKeysAfter).isZero();
    }

    // ──────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────

    /**
     * Inserts test countries into the PostgreSQL database.
     */
    private void seedTestCountries() {
        var usa = new Country("USA", "United States", "United States of America",
                "Washington, D.C.", "Americas", "Northern America",
                331_000_000L, 9_833_520.0, 38.0, -97.0, 38.8951, -77.0364,
                "https://flagcdn.com/w320/us.png", "https://flagcdn.com/us.svg");

        var mex = new Country("MEX", "Mexico", "United Mexican States",
                "Mexico City", "Americas", "Central America",
                128_900_000L, 1_964_375.0, 23.0, -102.0, 19.4326, -99.1332,
                "https://flagcdn.com/w320/mx.png", "https://flagcdn.com/mx.svg");

        var gbr = new Country("GBR", "United Kingdom", "United Kingdom of Great Britain and Northern Ireland",
                "London", "Europe", "Northern Europe",
                67_800_000L, 242_495.0, 54.0, -2.0, 51.5074, -0.1278,
                "https://flagcdn.com/w320/gb.png", "https://flagcdn.com/gb.svg");

        countryRepository.saveAll(List.of(usa, mex, gbr));
    }

    /**
     * Warms the Spring Cache by issuing a GET request to the countries endpoint.
     */
    private void warmCache() {
        restTemplate.exchange(
                "/api/v1/countries",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<CountryDTO>>() {}
        );
    }
}
