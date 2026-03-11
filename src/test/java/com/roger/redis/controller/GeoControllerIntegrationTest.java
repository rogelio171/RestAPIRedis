package com.roger.redis.controller;

import com.roger.redis.config.TestContainersConfig;
import com.roger.redis.model.dto.GeoSearchResult;
import com.roger.redis.model.entity.Country;
import com.roger.redis.repository.CountryRepository;
import com.roger.redis.seeder.DataSeeder;
import com.roger.redis.service.GeoService;

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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link GeoController}.
 *
 * <p>Uses Testcontainers (PostgreSQL + Redis) to run against real infrastructure.
 * Test countries are inserted into PostgreSQL and their centroid locations are
 * loaded into the Redis geospatial index via {@link GeoService} before each test.</p>
 *
 * @author Roger
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestContainersConfig.class)
@AutoConfigureTestRestTemplate
class GeoControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CountryRepository countryRepository;

    @Autowired
    private GeoService geoService;

    /** Prevents the {@link DataSeeder} from calling the external REST Countries API. */
    @MockitoBean
    private DataSeeder dataSeeder;

    @BeforeEach
    void setUp() {
        seedTestCountries();
        loadGeoData();
    }

    @AfterEach
    void tearDown() {
        countryRepository.deleteAll();
    }

    // ──────────────────────────────────────────────
    // Tests
    // ──────────────────────────────────────────────

    @Test
    void findNearby_shouldReturnCountriesWithinRadius() {
        var response = restTemplate.exchange(
                "/api/v1/geo/nearby?lat=38&lng=-97&radiusKm=3000",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<GeoSearchResult>>() {}
        );

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getDistance_shouldReturnDistanceBetweenCountries() {
        var response = restTemplate.getForEntity(
                "/api/v1/geo/distance?from=USA&to=MEX",
                Map.class
        );

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsKey("distanceKm");

        var distanceKm = ((Number) response.getBody().get("distanceKm")).doubleValue();
        assertThat(distanceKm).isGreaterThan(0);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getPosition_shouldReturnCoordinates() {
        var response = restTemplate.getForEntity(
                "/api/v1/geo/position/USA",
                Map.class
        );

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsKey("latitude");
        assertThat(response.getBody()).containsKey("longitude");
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

        var can = new Country("CAN", "Canada", "Canada",
                "Ottawa", "Americas", "Northern America",
                38_000_000L, 9_984_670.0, 56.0, -106.0, 45.4215, -75.6972,
                "https://flagcdn.com/w320/ca.png", "https://flagcdn.com/ca.svg");

        countryRepository.saveAll(List.of(usa, mex, can));
    }

    /**
     * Loads test country centroid locations into the Redis geospatial index.
     */
    private void loadGeoData() {
        geoService.addCountryLocation("USA", "United States", -97.0, 38.0);
        geoService.addCountryLocation("MEX", "Mexico", -102.0, 23.0);
        geoService.addCountryLocation("CAN", "Canada", -106.0, 56.0);
    }
}
