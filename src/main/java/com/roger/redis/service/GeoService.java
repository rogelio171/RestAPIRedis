package com.roger.redis.service;

import com.roger.redis.exception.ResourceNotFoundException;
import com.roger.redis.model.dto.GeoSearchResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.connection.RedisGeoCommands.GeoRadiusCommandArgs;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.domain.geo.Metrics;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Service class demonstrating Redis Geospatial features using {@link org.springframework.data.redis.core.GeoOperations}.
 *
 * <p>Redis geospatial indices allow storing latitude/longitude pairs associated with members
 * and performing proximity queries efficiently. Under the hood, Redis stores geospatial data
 * in a sorted set (ZSET) using a geohash-based score, which enables O(log N) lookups.</p>
 *
 * <h2>Redis Geospatial Commands Used</h2>
 * <ul>
 *   <li><strong>GEOADD</strong> — Adds one or more geospatial items (longitude, latitude, member)
 *       to a sorted set key. Coordinates are stored as a 52-bit geohash internally.
 *       Used by {@link #addCountryLocation} and {@link #addCapitalLocation}.</li>
 *   <li><strong>GEORADIUS</strong> — Queries members within a given radius from a point,
 *       optionally returning distances and coordinates, with sorting and limiting support.
 *       Used by {@link #findNearbyCountries}.</li>
 *   <li><strong>GEODIST</strong> — Returns the distance between two members in the geospatial
 *       index, in the specified unit (e.g., kilometres). Used by {@link #getDistanceBetween}.</li>
 *   <li><strong>GEOPOS</strong> — Returns the longitude and latitude of one or more members.
 *       Used by {@link #getPosition}.</li>
 *   <li><strong>ZCARD</strong> — Because geospatial indices are stored as sorted sets, ZCARD
 *       returns the number of members in the index. Used by {@link #getGeoIndexSize}.</li>
 * </ul>
 *
 * <h2>Important Notes</h2>
 * <ul>
 *   <li>Redis {@link Point} uses <strong>(longitude, latitude)</strong> order — longitude is X,
 *       latitude is Y. This is the opposite of many geographic conventions.</li>
 *   <li>Country names are stored in a separate Redis hash ({@code geo:names}) because geospatial
 *       indices only store the member key (country code), not arbitrary metadata.</li>
 * </ul>
 *
 * @author Roger
 * @see org.springframework.data.redis.core.GeoOperations
 * @see RedisGeoCommands
 */
@Service
public class GeoService {

    private static final Logger log = LoggerFactory.getLogger(GeoService.class);

    /** Redis key for the geospatial index of country centroid locations. */
    private static final String GEO_KEY = "geo:countries";

    /** Redis key for the geospatial index of capital city locations. */
    private static final String GEO_CAPITALS_KEY = "geo:capitals";

    /** Redis hash key for mapping country codes to country names. */
    private static final String GEO_NAMES_KEY = "geo:names";

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Constructs a new {@code GeoService} with the given {@link RedisTemplate}.
     *
     * @param redisTemplate the Redis template configured in
     *                      {@link com.roger.redis.config.RedisConfig}
     */
    public GeoService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Adds a country's centroid location to the geospatial index.
     *
     * <p>Executes a Redis {@code GEOADD} command to store the coordinates under
     * the {@value #GEO_KEY} key, and an {@code HSET} command to associate the
     * country code with its human-readable name in the {@value #GEO_NAMES_KEY} hash.</p>
     *
     * @param countryCode the ISO 3166-1 alpha-3 country code (used as the geo member)
     * @param countryName the human-readable country name
     * @param longitude   the longitude of the country's centroid
     * @param latitude    the latitude of the country's centroid
     */
    public void addCountryLocation(String countryCode, String countryName, double longitude, double latitude) {
        log.info("Adding geospatial location for country {} ({}) at ({}, {})",
                countryCode, countryName, longitude, latitude);

        redisTemplate.opsForGeo().add(GEO_KEY, new Point(longitude, latitude), countryCode);
        redisTemplate.opsForHash().put(GEO_NAMES_KEY, countryCode, countryName);

        log.debug("Successfully added geo location and name mapping for {}", countryCode);
    }

    /**
     * Adds a capital city's location to the capitals geospatial index.
     *
     * <p>Executes a Redis {@code GEOADD} command to store the capital coordinates
     * under the {@value #GEO_CAPITALS_KEY} key.</p>
     *
     * @param countryCode the ISO 3166-1 alpha-3 country code (used as the geo member)
     * @param longitude   the longitude of the capital city
     * @param latitude    the latitude of the capital city
     */
    public void addCapitalLocation(String countryCode, double longitude, double latitude) {
        log.info("Adding capital location for country {} at ({}, {})", countryCode, longitude, latitude);

        redisTemplate.opsForGeo().add(GEO_CAPITALS_KEY, new Point(longitude, latitude), countryCode);

        log.debug("Successfully added capital geo location for {}", countryCode);
    }

    /**
     * Finds countries within a given radius of the specified coordinates.
     *
     * <p>Executes a Redis {@code GEORADIUS} command against the {@value #GEO_KEY} index.
     * Results include the distance from the search origin and the stored coordinates of
     * each match. Country names are resolved from the {@value #GEO_NAMES_KEY} hash.</p>
     *
     * @param latitude  the latitude of the search origin
     * @param longitude the longitude of the search origin
     * @param radiusKm  the search radius in kilometres
     * @param limit     the maximum number of results to return
     * @return a list of {@link GeoSearchResult} entries sorted by ascending distance,
     *         or an empty list if no matches are found
     */
    public List<GeoSearchResult> findNearbyCountries(double latitude, double longitude,
                                                     double radiusKm, int limit) {
        log.info("Searching for countries within {} km of ({}, {}), limit {}",
                radiusKm, latitude, longitude, limit);

        GeoResults<RedisGeoCommands.GeoLocation<Object>> results = redisTemplate.opsForGeo().radius(
                GEO_KEY,
                new Circle(
                        new Point(longitude, latitude),
                        new Distance(radiusKm, Metrics.KILOMETERS)
                ),
                GeoRadiusCommandArgs.newGeoRadiusArgs()
                        .includeDistance()
                        .includeCoordinates()
                        .sortAscending()
                        .limit(limit)
        );

        if (results == null) {
            log.debug("No geospatial results returned (null)");
            return Collections.emptyList();
        }

        List<GeoSearchResult> searchResults = results.getContent().stream()
                .map(result -> {
                    String countryCode = result.getContent().getName().toString();
                    Point point = result.getContent().getPoint();
                    double distanceKm = result.getDistance().getValue();

                    Object nameObj = redisTemplate.opsForHash().get(GEO_NAMES_KEY, countryCode);
                    String countryName = nameObj != null ? nameObj.toString() : countryCode;

                    return new GeoSearchResult(
                            countryCode,
                            countryName,
                            point.getY(),
                            point.getX(),
                            distanceKm
                    );
                })
                .toList();

        log.info("Found {} countries within {} km radius", searchResults.size(), radiusKm);
        return searchResults;
    }

    /**
     * Calculates the distance between two countries in the geospatial index.
     *
     * <p>Executes a Redis {@code GEODIST} command to compute the great-circle distance
     * between the two members identified by their country codes.</p>
     *
     * @param code1 the ISO 3166-1 alpha-3 code of the first country
     * @param code2 the ISO 3166-1 alpha-3 code of the second country
     * @return the distance in kilometres, or {@code null} if either country code
     *         is not present in the geospatial index
     */
    public Double getDistanceBetween(String code1, String code2) {
        log.info("Calculating distance between {} and {}", code1, code2);

        Distance distance = redisTemplate.opsForGeo().distance(GEO_KEY, code1, code2, Metrics.KILOMETERS);

        if (distance == null) {
            log.warn("Could not calculate distance — one or both codes ({}, {}) not in geo index",
                    code1, code2);
            return null;
        }

        log.info("Distance between {} and {}: {} km", code1, code2, distance.getValue());
        return distance.getValue();
    }

    /**
     * Retrieves the stored coordinates of a country from the geospatial index.
     *
     * <p>Executes a Redis {@code GEOPOS} command to look up the longitude and latitude
     * associated with the given country code.</p>
     *
     * @param countryCode the ISO 3166-1 alpha-3 country code
     * @return a map with keys {@code "latitude"} and {@code "longitude"}
     * @throws ResourceNotFoundException if the country code is not found in the geospatial index
     */
    public Map<String, Double> getPosition(String countryCode) {
        log.info("Retrieving geospatial position for country {}", countryCode);

        List<Point> positions = redisTemplate.opsForGeo().position(GEO_KEY, countryCode);

        if (positions == null || positions.isEmpty() || positions.getFirst() == null) {
            log.warn("Country code {} not found in geospatial index", countryCode);
            throw new ResourceNotFoundException("GeoLocation", "countryCode", countryCode);
        }

        Point point = positions.getFirst();
        log.debug("Position for {}: longitude={}, latitude={}", countryCode, point.getX(), point.getY());

        return Map.of(
                "latitude", point.getY(),
                "longitude", point.getX()
        );
    }

    /**
     * Returns the number of members in the geospatial index.
     *
     * <p>Because Redis geospatial indices are stored as sorted sets (ZSETs),
     * this method uses {@code ZCARD} to determine the total count of indexed locations.</p>
     *
     * @return the number of entries in the {@value #GEO_KEY} geospatial index,
     *         or {@code 0} if the key does not exist
     */
    public Long getGeoIndexSize() {
        Long size = redisTemplate.opsForZSet().size(GEO_KEY);
        log.info("Geo index '{}' contains {} entries", GEO_KEY, size);
        return size;
    }
}
