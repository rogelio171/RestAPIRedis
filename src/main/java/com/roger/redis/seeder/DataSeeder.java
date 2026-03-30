package com.roger.redis.seeder;

import tools.jackson.databind.json.JsonMapper;
import com.roger.redis.model.entity.Country;
import com.roger.redis.repository.CountryRepository;
import com.roger.redis.service.GeoService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

/**
 * Seeds the PostgreSQL database and Redis geospatial index on application startup.
 *
 * <p>Fetches country data from the REST Countries API, persists each country as a
 * {@link Country} entity via JPA, and then populates the Redis geo indices (country
 * centroids and capital city locations) through {@link GeoService}.</p>
 *
 * <p>The seeder is idempotent — if the database already contains data, it skips
 * seeding entirely to avoid duplicates on subsequent restarts.</p>
 *
 * @author Roger
 */
@Component
@Order(1)
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    /** REST Countries API endpoint with the required field projections. */
    private static final String API_URL =
            "https://restcountries.com/v3.1/all?fields=name,cca3,capital,latlng,capitalInfo,population,area,region,subregion,flags";

    private final CountryRepository countryRepository;
    private final GeoService geoService;
    private final RestClient.Builder restClientBuilder;

    /**
     * Constructs a new {@code DataSeeder} with the required dependencies.
     *
     * @param countryRepository the JPA repository for {@link Country} entities
     * @param geoService        the service for managing Redis geospatial indices
     * @param restClientBuilder the Spring Boot auto-configured {@link RestClient.Builder}
     */
    public DataSeeder(CountryRepository countryRepository, GeoService geoService,
                      RestClient.Builder restClientBuilder) {
        this.countryRepository = countryRepository;
        this.geoService = geoService;
        this.restClientBuilder = restClientBuilder;
    }

    /**
     * Executes the seeding logic when the application starts.
     *
     * <ol>
     *   <li>Checks whether the database is already seeded; if so, logs and returns early.</li>
     *   <li>Fetches all countries from the REST Countries API.</li>
     *   <li>Maps the JSON response to {@link Country} entities.</li>
     *   <li>Bulk-saves all entities to PostgreSQL.</li>
     *   <li>Loads geospatial data into Redis for country centroids and capital cities.</li>
     * </ol>
     *
     * @param args command-line arguments (ignored)
     */
    @Override
    public void run(String... args) {
        if (countryRepository.count() > 0) {
            log.info("Database already seeded — skipping data import and syncing Redis geo index from PostgreSQL.");
            syncGeoIndex(countryRepository.findAll());
            return;
        }

        try {
            var restClient = restClientBuilder.build();
            var response = fetchCountryData(restClient);

            var mapper = new JsonMapper();
            var countries = mapper.readTree(response);

            var countryList = new ArrayList<Country>();
            var skippedCountries = 0;

            for (var node : countries) {
                try {
                    var cca3 = node.path("cca3").asText("");
                    var commonName = node.path("name").path("common").asText("");
                    var officialName = node.path("name").path("official").asText("");

                    var capitalArray = node.path("capital");
                    var capital = (capitalArray.isArray() && !capitalArray.isEmpty())
                            ? capitalArray.get(0).asText("")
                            : "";

                    var region = node.path("region").asText("");
                    var subregion = node.path("subregion").asText("");
                    var population = node.path("population").asLong(0);
                    var area = node.path("area").asDouble(0.0);

                    var latlng = node.path("latlng");
                    var latitude = (latlng.isArray() && latlng.size() >= 2)
                            ? latlng.get(0).asDouble(0.0)
                            : 0.0;
                    var longitude = (latlng.isArray() && latlng.size() >= 2)
                            ? latlng.get(1).asDouble(0.0)
                            : 0.0;

                    var capitalInfoLatlng = node.path("capitalInfo").path("latlng");
                    var capitalLat = (capitalInfoLatlng.isArray() && capitalInfoLatlng.size() >= 2)
                            ? capitalInfoLatlng.get(0).asDouble(0.0)
                            : 0.0;
                    var capitalLng = (capitalInfoLatlng.isArray() && capitalInfoLatlng.size() >= 2)
                            ? capitalInfoLatlng.get(1).asDouble(0.0)
                            : 0.0;

                    var flagUrl = node.path("flags").path("png").asText("");
                    var flagSvg = node.path("flags").path("svg").asText("");

                    countryList.add(new Country(
                            cca3, commonName, officialName, capital,
                            region, subregion, population, area,
                            latitude, longitude, capitalLat, capitalLng,
                            flagUrl, flagSvg
                    ));
                } catch (Exception e) {
                    skippedCountries++;
                    var cca3 = node.path("cca3").asText("unknown");
                    var name = node.path("name").path("common").asText("unknown");
                    log.warn("Skipped country (cca3={}, name={}): {}", cca3, name, e.getMessage());
                }
            }

            if (skippedCountries > 0) {
                log.warn("Skipped {} countries due to parsing errors", skippedCountries);
            }

            var savedCountries = countryRepository.saveAll(countryList);
            log.info("Seeded {} countries into PostgreSQL", savedCountries.size());

            syncGeoIndex(savedCountries);

        } catch (Exception e) {
            log.error("Failed to seed database: {}", e.getMessage(), e);
        }
    }

    private void syncGeoIndex(List<Country> countries) {
        int geoSuccess = 0;
        int geoFailures = 0;

        for (var country : countries) {
            try {
                geoService.addCountryLocation(country.getCca3(), country.getCommonName(),
                        country.getLongitude(), country.getLatitude());
                geoSuccess++;

                if (country.getCapitalLat() != 0.0 || country.getCapitalLng() != 0.0) {
                    geoService.addCapitalLocation(country.getCca3(),
                            country.getCapitalLng(), country.getCapitalLat());
                }
            } catch (Exception e) {
                geoFailures++;
                log.warn("Failed to geo-index country {}: {}", country.getCca3(), e.getMessage());
            }
        }

        log.info("Loaded {}/{} country locations into Redis geo index ({} failures)",
                geoSuccess, countries.size(), geoFailures);
    }

    /**
     * Fetches country data from the REST Countries API with retry logic.
     *
     * <p>Attempts the HTTP call up to 3 times with exponential backoff
     * (2s, 4s, 8s) between attempts. If all attempts fail, the last
     * exception is rethrown wrapped in a {@link RuntimeException}.</p>
     *
     * @param restClient the configured {@link RestClient} to use
     * @return the raw JSON response body
     * @throws RuntimeException if all retry attempts are exhausted
     */
    private String fetchCountryData(RestClient restClient) {
        int maxAttempts = 3;
        long[] backoffMs = {2000, 4000, 8000};
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                log.info("Fetching country data (attempt {}/{})", attempt, maxAttempts);
                return restClient.get()
                        .uri(API_URL)
                        .retrieve()
                        .body(String.class);
            } catch (Exception e) {
                lastException = e;
                log.warn("Attempt {}/{} failed: {}", attempt, maxAttempts, e.getMessage());
                if (attempt < maxAttempts) {
                    try {
                        Thread.sleep(backoffMs[attempt - 1]);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted during retry backoff", ie);
                    }
                }
            }
        }
        throw new RuntimeException("Failed to fetch country data after " + maxAttempts + " attempts", lastException);
    }
}
