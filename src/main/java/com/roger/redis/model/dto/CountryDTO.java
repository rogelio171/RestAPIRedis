package com.roger.redis.model.dto;

import com.roger.redis.model.entity.Country;

import java.io.Serial;
import java.io.Serializable;

/**
 * Immutable DTO for country API responses.
 *
 * <p>Implements {@link Serializable} to support Kryo serialization for Redis caching.
 * Use the {@link #fromEntity(Country)} factory method to convert a JPA entity into this record.</p>
 *
 * @param cca3         ISO 3166-1 alpha-3 country code
 * @param commonName   common (short) name of the country
 * @param officialName official (formal) name of the country
 * @param capital      name of the capital city
 * @param region       geographic region
 * @param subregion    geographic subregion
 * @param population   total population
 * @param area         total area in square kilometres
 * @param latitude     country centroid latitude
 * @param longitude    country centroid longitude
 * @param capitalLat   capital city latitude
 * @param capitalLng   capital city longitude
 * @param flagUrl      URL to the PNG flag image
 * @param flagSvg      URL to the SVG flag image
 *
 * @author Roger
 */
public record CountryDTO(
        String cca3,
        String commonName,
        String officialName,
        String capital,
        String region,
        String subregion,
        long population,
        double area,
        double latitude,
        double longitude,
        double capitalLat,
        double capitalLng,
        String flagUrl,
        String flagSvg
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Creates a {@code CountryDTO} from a {@link Country} JPA entity.
     *
     * @param country the entity to convert; must not be {@code null}
     * @return a new {@code CountryDTO} populated from the given entity
     * @throws NullPointerException if {@code country} is {@code null}
     */
    public static CountryDTO fromEntity(Country country) {
        return new CountryDTO(
                country.getCca3(),
                country.getCommonName(),
                country.getOfficialName(),
                country.getCapital(),
                country.getRegion(),
                country.getSubregion(),
                country.getPopulation(),
                country.getArea(),
                country.getLatitude(),
                country.getLongitude(),
                country.getCapitalLat(),
                country.getCapitalLng(),
                country.getFlagUrl(),
                country.getFlagSvg()
        );
    }
}
