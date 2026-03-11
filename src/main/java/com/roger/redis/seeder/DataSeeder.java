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
            log.info("Database already seeded — skipping data import.");
            return;
        }

        try {
            var restClient = restClientBuilder.build();

            log.info("Fetching country data from REST Countries API...");
            var response = restClient.get()
                    .uri(API_URL)
                    .retrieve()
                    .body(String.class);

            var mapper = new JsonMapper();
            var countries = mapper.readTree(response);

            var countryList = new ArrayList<Country>();

            for (var node : countries) {
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
            }

            var savedCountries = countryRepository.saveAll(countryList);
            log.info("Seeded {} countries into PostgreSQL", savedCountries.size());

            var geoCount = 0;
            for (var country : savedCountries) {
                geoService.addCountryLocation(country.getCca3(), country.getCommonName(),
                        country.getLongitude(), country.getLatitude());
                geoCount++;

                if (country.getCapitalLat() != 0.0 || country.getCapitalLng() != 0.0) {
                    geoService.addCapitalLocation(country.getCca3(),
                            country.getCapitalLng(), country.getCapitalLat());
                }
            }

            log.info("Loaded {} country locations into Redis geo index", geoCount);

        } catch (Exception e) {
            log.error("Failed to seed database: {}", e.getMessage(), e);
        }
    }
}
