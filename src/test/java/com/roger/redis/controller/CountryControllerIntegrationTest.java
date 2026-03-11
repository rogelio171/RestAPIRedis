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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link CountryController}.
 *
 * <p>Uses Testcontainers (PostgreSQL + Redis) to run against real infrastructure.
 * The {@link DataSeeder} is mocked out to prevent external API calls on startup;
 * test data is inserted directly via the {@link CountryRepository}.</p>
 *
 * @author Roger
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "app.security.mode=disabled")
@Import({TestContainersConfig.class, TestSecurityConfig.class})
@AutoConfigureTestRestTemplate
class CountryControllerIntegrationTest {

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
    }

    @AfterEach
    void tearDown() {
        countryRepository.deleteAll();
    }

    // ──────────────────────────────────────────────
    // Tests
    // ──────────────────────────────────────────────

    @Test
    void getAllCountries_shouldReturnAllCountries() {
        var response = restTemplate.exchange(
                "/api/v1/countries",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<CountryDTO>>() {}
        );

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull().hasSize(4);
        assertThat(response.getHeaders().get("X-Response-Time-Ms")).isNotNull().isNotEmpty();
    }

    @Test
    void getByCode_shouldReturnCountry() {
        var response = restTemplate.getForEntity("/api/v1/countries/USA", CountryDTO.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().cca3()).isEqualTo("USA");
        assertThat(response.getBody().commonName()).isEqualTo("United States");
    }

    @Test
    void getByCode_shouldReturn404_whenNotFound() {
        var response = restTemplate.getForEntity("/api/v1/countries/XYZ", String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void getByRegion_shouldReturnCountriesInRegion() {
        var response = restTemplate.exchange(
                "/api/v1/countries/region/Americas",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<CountryDTO>>() {}
        );

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();

        var codes = response.getBody().stream().map(CountryDTO::cca3).toList();
        assertThat(codes).containsExactlyInAnyOrder("USA", "MEX");
    }

    @Test
    void searchByName_shouldReturnMatchingCountries() {
        var response = restTemplate.exchange(
                "/api/v1/countries/search?name=united",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<CountryDTO>>() {}
        );

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();

        var codes = response.getBody().stream().map(CountryDTO::cca3).toList();
        assertThat(codes).containsExactlyInAnyOrder("USA", "MEX", "GBR");
    }

    @Test
    @SuppressWarnings("unchecked")
    void refresh_shouldEvictCaches() {
        var response = restTemplate.postForEntity(
                "/api/v1/countries/refresh",
                null,
                Map.class
        );

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsKey("message");
    }

    // ──────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────

    /**
     * Inserts four test countries (USA, MEX, GBR, JPN) into the database.
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

        var jpn = new Country("JPN", "Japan", "Japan",
                "Tokyo", "Asia", "Eastern Asia",
                126_500_000L, 377_975.0, 36.0, 138.0, 35.6762, 139.6503,
                "https://flagcdn.com/w320/jp.png", "https://flagcdn.com/jp.svg");

        countryRepository.saveAll(List.of(usa, mex, gbr, jpn));
    }
}
