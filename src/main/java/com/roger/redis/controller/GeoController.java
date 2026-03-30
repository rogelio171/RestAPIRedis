package com.roger.redis.controller;

import com.roger.redis.exception.ResourceNotFoundException;
import com.roger.redis.model.dto.GeoSearchResult;
import com.roger.redis.service.GeoService;
import com.roger.redis.util.ResponseTimer;

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

    public GeoController(GeoService geoService) {
        this.geoService = geoService;
    }

    /**
     * Finds countries near a given geographic point within a specified radius.
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

        var timer = ResponseTimer.start();
        var results = geoService.findNearbyCountries(lat, lng, radiusKm, limit);

        return ResponseEntity.ok()
                .header("X-Response-Time-Ms", timer.elapsedMsString())
                .body(results);
    }

    /**
     * Calculates the distance between two countries identified by their ISO country codes.
     *
     * @param from the ISO 3166-1 alpha-3 code of the origin country
     * @param to   the ISO 3166-1 alpha-3 code of the destination country
     * @return a {@link ResponseEntity} containing distance information
     * @throws ResourceNotFoundException if either country code is not in the geo index
     */
    @GetMapping("/distance")
    public ResponseEntity<Map<String, Object>> getDistance(
            @RequestParam String from,
            @RequestParam String to) {

        var distance = geoService.getDistanceBetween(from, to);

        if (distance == null) {
            throw new ResourceNotFoundException("GeoDistance", "countryCodes", from + ", " + to);
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
     * @param code the ISO 3166-1 alpha-3 country code
     * @return a {@link ResponseEntity} containing latitude and longitude
     */
    @GetMapping("/position/{code}")
    public ResponseEntity<Map<String, Double>> getPosition(@PathVariable String code) {
        var position = geoService.getPosition(code);
        return ResponseEntity.ok(position);
    }

    /**
     * Returns statistics about the Redis geospatial index.
     *
     * @return a {@link ResponseEntity} containing the number of indexed countries
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        var size = geoService.getGeoIndexSize();

        var stats = Map.<String, Object>of(
                "countriesIndexed", size
        );

        return ResponseEntity.ok(stats);
    }
}
