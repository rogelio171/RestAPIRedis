package com.roger.redis.controller;

import com.roger.redis.model.dto.GeoSearchResult;
import com.roger.redis.service.GeoService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * REST controller exposing Redis Geospatial operations for country location data.
 *
 * <p>This controller provides endpoints that leverage Redis geospatial commands
 * ({@code GEORADIUS}, {@code GEODIST}, {@code GEOPOS}, {@code ZCARD}) to perform
 * proximity searches, distance calculations, coordinate lookups, and index statistics
 * against country centroid data stored in a Redis geospatial index.</p>
 *
 * <p>All endpoints are prefixed with {@code /api/v1/geo}.</p>
 *
 * @author Roger
 * @see GeoService
 */
@RestController
@RequestMapping("/api/v1/geo")
public class GeoController {

    private final GeoService geoService;

    /**
     * Constructs a new {@code GeoController} with the given {@link GeoService}.
     *
     * @param geoService the geospatial service used to interact with the Redis geo index
     */
    public GeoController(GeoService geoService) {
        this.geoService = geoService;
    }

    /**
     * Finds countries near a given geographic point within a specified radius.
     *
     * <p>Delegates to {@link GeoService#findNearbyCountries}, which executes a Redis
     * {@code GEORADIUS} command. The command searches the geospatial index for members
     * within the given radius of the provided coordinates, returning results sorted
     * by ascending distance. Each result includes the member's coordinates and distance
     * from the search origin.</p>
     *
     * <p>The response includes an {@code X-Response-Time-Ms} header indicating the
     * server-side processing time in milliseconds.</p>
     *
     * @param lat      the latitude of the search origin
     * @param lng      the longitude of the search origin
     * @param radiusKm the search radius in kilometres (default: 1000)
     * @param limit    the maximum number of results to return (default: 10)
     * @return a {@link ResponseEntity} containing a list of {@link GeoSearchResult} entries
     */
    @GetMapping("/nearby")
    public ResponseEntity<List<GeoSearchResult>> findNearbyCountries(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "1000") double radiusKm,
            @RequestParam(defaultValue = "10") int limit) {

        var startNanos = System.nanoTime();

        var results = geoService.findNearbyCountries(lat, lng, radiusKm, limit);

        return ResponseEntity.ok()
                .header("X-Response-Time-Ms", String.valueOf(elapsedMs(startNanos)))
                .body(results);
    }

    /**
     * Calculates the distance between two countries identified by their ISO country codes.
     *
     * <p>Delegates to {@link GeoService#getDistanceBetween}, which executes a Redis
     * {@code GEODIST} command. The command computes the great-circle distance between
     * two members stored in the geospatial index using the Haversine formula, returning
     * the result in kilometres.</p>
     *
     * <p>Returns a {@code 404 Not Found} response if either country code is not present
     * in the geospatial index.</p>
     *
     * @param from the ISO 3166-1 alpha-3 code of the origin country
     * @param to   the ISO 3166-1 alpha-3 code of the destination country
     * @return a {@link ResponseEntity} containing a map with keys {@code "from"}, {@code "to"},
     *         {@code "distanceKm"}, and {@code "unit"}
     */
    @GetMapping("/distance")
    public ResponseEntity<Map<String, Object>> getDistance(
            @RequestParam String from,
            @RequestParam String to) {

        var distance = geoService.getDistanceBetween(from, to);

        if (distance == null) {
            return ResponseEntity.notFound()
                    .header("X-Error", "One or both country codes not found in geo index: " + from + ", " + to)
                    .build();
        }

        var result = Map.<String, Object>of(
                "from", from,
                "to", to,
                "distanceKm", distance,
                "unit", "km"
        );

        return ResponseEntity.ok(result);
    }

    /**
     * Retrieves the stored coordinates of a country from the Redis geospatial index.
     *
     * <p>Delegates to {@link GeoService#getPosition}, which executes a Redis {@code GEOPOS}
     * command. The command returns the longitude and latitude associated with the given
     * member in the geospatial index. A {@link com.roger.redis.exception.ResourceNotFoundException}
     * is thrown by the service layer if the country code is not found.</p>
     *
     * @param code the ISO 3166-1 alpha-3 country code
     * @return a {@link ResponseEntity} containing a map with keys {@code "latitude"} and
     *         {@code "longitude"}
     */
    @GetMapping("/position/{code}")
    public ResponseEntity<Map<String, Double>> getPosition(@PathVariable String code) {
        var position = geoService.getPosition(code);
        return ResponseEntity.ok(position);
    }

    /**
     * Returns statistics about the Redis geospatial index.
     *
     * <p>Delegates to {@link GeoService#getGeoIndexSize}, which executes a Redis {@code ZCARD}
     * command. Because Redis geospatial indices are stored as sorted sets (ZSETs), {@code ZCARD}
     * returns the total number of members (countries) indexed.</p>
     *
     * @return a {@link ResponseEntity} containing a map with key {@code "countriesIndexed"}
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        var size = geoService.getGeoIndexSize();

        var stats = Map.<String, Object>of(
                "countriesIndexed", size
        );

        return ResponseEntity.ok(stats);
    }

    /**
     * Calculates the elapsed time in milliseconds from a starting nanoTime reference.
     *
     * @param startNanos the starting point obtained from {@link System#nanoTime()}
     * @return the elapsed time in milliseconds
     */
    private long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
