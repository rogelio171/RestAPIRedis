package com.roger.redis.controller;

import com.roger.redis.model.dto.CountryDTO;
import com.roger.redis.service.CountryService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * REST controller that exposes country-related endpoints.
 *
 * <p>All responses include an {@code X-Response-Time-Ms} custom header that
 * reports the server-side processing time in milliseconds, making it easy to
 * observe the performance improvement when Redis cache hits occur.</p>
 *
 * <p>List endpoints also include an {@code X-Total-Count} header with the
 * number of items in the response body.</p>
 *
 * @author Roger
 * @see CountryService
 * @see CountryDTO
 */
@RestController
@RequestMapping("/api/v1/countries")
public class CountryController {

    private final CountryService countryService;

    /**
     * Constructs a new {@code CountryController} with the required service dependency.
     *
     * @param countryService the service that handles country business logic and caching
     */
    public CountryController(CountryService countryService) {
        this.countryService = countryService;
    }

    /**
     * Returns all countries.
     *
     * <p>Includes {@code X-Response-Time-Ms} and {@code X-Total-Count} response headers.</p>
     *
     * @return a {@link ResponseEntity} containing the list of all {@link CountryDTO}s
     */
    @GetMapping
    public ResponseEntity<List<CountryDTO>> getAllCountries() {
        var start = System.nanoTime();
        var countries = countryService.getAllCountries();
        return ResponseEntity.ok()
                .header("X-Response-Time-Ms", String.valueOf(elapsedMs(start)))
                .header("X-Total-Count", String.valueOf(countries.size()))
                .body(countries);
    }

    /**
     * Returns a single country identified by its ISO 3166-1 alpha-3 code.
     *
     * <p>Includes an {@code X-Response-Time-Ms} response header.</p>
     *
     * @param code the three-letter country code (e.g. "USA", "MEX")
     * @return a {@link ResponseEntity} containing the matching {@link CountryDTO}
     */
    @GetMapping("/{code}")
    public ResponseEntity<CountryDTO> getByCode(@PathVariable String code) {
        var start = System.nanoTime();
        var country = countryService.getByCode(code);
        return ResponseEntity.ok()
                .header("X-Response-Time-Ms", String.valueOf(elapsedMs(start)))
                .body(country);
    }

    /**
     * Returns all countries belonging to the specified geographic region.
     *
     * <p>Includes {@code X-Response-Time-Ms} and {@code X-Total-Count} response headers.</p>
     *
     * @param region the region name (e.g. "Americas", "Europe", "Asia")
     * @return a {@link ResponseEntity} containing the list of matching {@link CountryDTO}s
     */
    @GetMapping("/region/{region}")
    public ResponseEntity<List<CountryDTO>> getByRegion(@PathVariable String region) {
        var start = System.nanoTime();
        var countries = countryService.getByRegion(region);
        return ResponseEntity.ok()
                .header("X-Response-Time-Ms", String.valueOf(elapsedMs(start)))
                .header("X-Total-Count", String.valueOf(countries.size()))
                .body(countries);
    }

    /**
     * Searches for countries whose name contains the given term (case-insensitive).
     *
     * <p>Includes {@code X-Response-Time-Ms} and {@code X-Total-Count} response headers.</p>
     *
     * @param name the search term to match against country names
     * @return a {@link ResponseEntity} containing the list of matching {@link CountryDTO}s
     */
    @GetMapping("/search")
    public ResponseEntity<List<CountryDTO>> searchByName(@RequestParam String name) {
        var start = System.nanoTime();
        var countries = countryService.searchByName(name);
        return ResponseEntity.ok()
                .header("X-Response-Time-Ms", String.valueOf(elapsedMs(start)))
                .header("X-Total-Count", String.valueOf(countries.size()))
                .body(countries);
    }

    /**
     * Evicts all country-related caches.
     *
     * <p>Use this endpoint after bulk data changes to ensure stale data is not
     * served from Redis.</p>
     *
     * @return a {@link ResponseEntity} containing a confirmation message
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refreshAll() {
        countryService.refreshAll();
        return ResponseEntity.ok(Map.of("message", "All country caches evicted successfully"));
    }

    /**
     * Returns the total number of countries in the database.
     *
     * @return a {@link ResponseEntity} containing a map with the key {@code "count"}
     */
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getCount() {
        var count = countryService.getCount();
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * Calculates the elapsed time in milliseconds from a {@link System#nanoTime()} start value.
     *
     * @param startNanos the nanoTime recorded before the operation
     * @return elapsed time in milliseconds (as a {@code double} for sub-millisecond precision)
     */
    private double elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000.0;
    }
}
