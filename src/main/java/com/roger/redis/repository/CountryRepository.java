package com.roger.redis.repository;

import com.roger.redis.model.entity.Country;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for the {@link Country} entity.
 *
 * <p>Provides CRUD operations inherited from {@link JpaRepository} along with
 * custom finder and search methods for querying countries by ISO code, region,
 * subregion, population, and name.</p>
 *
 * @author Roger
 */
public interface CountryRepository extends JpaRepository<Country, Long> {

    /**
     * Finds a country by its ISO 3166-1 alpha-3 code.
     *
     * @param cca3 the three-letter country code (e.g. "USA", "MEX", "GBR")
     * @return an {@link Optional} containing the matching country, or empty if none found
     */
    Optional<Country> findByCca3(String cca3);

    /**
     * Finds all countries belonging to the specified geographic region.
     *
     * @param region the region name (e.g. "Americas", "Europe", "Asia")
     * @return a list of countries in the given region; empty list if none found
     */
    List<Country> findByRegion(String region);

    /**
     * Finds all countries belonging to the specified geographic subregion.
     *
     * @param subregion the subregion name (e.g. "Northern America", "Western Europe")
     * @return a list of countries in the given subregion; empty list if none found
     */
    List<Country> findBySubregion(String subregion);

    /**
     * Finds all countries whose population exceeds the given threshold.
     *
     * @param population the minimum population threshold (exclusive)
     * @return a list of countries with population greater than the threshold; empty list if none found
     */
    List<Country> findByPopulationGreaterThan(long population);

    /**
     * Searches for countries whose common name or official name contains the given
     * string, using a case-insensitive partial match.
     *
     * @param name the search term to match against common and official names
     * @return a list of countries matching the search term; empty list if none found
     */
    @Query("SELECT c FROM Country c WHERE LOWER(c.commonName) LIKE LOWER(CONCAT('%', :name, '%')) OR LOWER(c.officialName) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Country> searchByName(@Param("name") String name);
}
