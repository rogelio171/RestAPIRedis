package com.roger.redis.model.dto;

import java.io.Serial;
import java.io.Serializable;

/**
 * Immutable DTO representing the result of a geospatial search query.
 *
 * <p>Contains the matched country's code, name, coordinates, and the calculated
 * distance (in kilometres) from the search origin. Implements {@link Serializable}
 * to support Kryo serialization for Redis caching.</p>
 *
 * @param countryCode ISO 3166-1 alpha-3 country code of the matched country
 * @param countryName common name of the matched country
 * @param latitude    latitude of the matched country's centroid
 * @param longitude   longitude of the matched country's centroid
 * @param distanceKm  distance from the search origin in kilometres
 *
 * @author Roger
 */
public record GeoSearchResult(
        String countryCode,
        String countryName,
        double latitude,
        double longitude,
        double distanceKm
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
