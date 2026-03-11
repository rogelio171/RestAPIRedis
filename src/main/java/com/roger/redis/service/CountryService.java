package com.roger.redis.service;

import com.roger.redis.exception.ResourceNotFoundException;
import com.roger.redis.model.dto.CountryDTO;
import com.roger.redis.model.entity.Country;
import com.roger.redis.repository.CountryRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service layer for country operations with Redis-backed caching.
 *
 * <p>This service sits between the REST controller and the {@link CountryRepository},
 * applying Spring Cache annotations so that frequently-requested data is served from
 * Redis rather than hitting the database on every call.</p>
 *
 * <h3>Caching strategy</h3>
 * <ul>
 *   <li><b>{@code countries}</b> — caches individual country lookups (by code) and the
 *       full country list. TTL is governed by the {@code countries} cache configuration
 *       defined in {@link com.roger.redis.config.RedisConfig}.</li>
 *   <li><b>{@code countries:byRegion}</b> — caches region-based queries, keyed by the
 *       lower-cased region name.</li>
 *   <li><b>{@code countries:search}</b> — caches name-search results, keyed by the
 *       lower-cased search term.</li>
 * </ul>
 *
 * <p>All caches can be evicted at once via {@link #refreshAll()}.</p>
 *
 * @author Roger
 * @see CountryRepository
 * @see CountryDTO
 */
@Service
public class CountryService {

    private static final Logger log = LoggerFactory.getLogger(CountryService.class);

    private final CountryRepository countryRepository;

    /**
     * Constructs a new {@code CountryService} with the required repository dependency.
     *
     * @param countryRepository the JPA repository for {@link Country} entities
     */
    public CountryService(CountryRepository countryRepository) {
        this.countryRepository = countryRepository;
    }

    /**
     * Returns every country in the database as a list of DTOs.
     *
     * <p><b>Cache:</b> stored in the {@code countries} cache under the fixed key
     * {@code "all"}. Subsequent calls return the cached list until the cache entry
     * expires (TTL defined in Redis configuration) or is explicitly evicted via
     * {@link #refreshAll()}.</p>
     *
     * @return an unmodifiable list of all {@link CountryDTO}s
     */
    @Cacheable(cacheNames = "countries", key = "'all'")
    public List<CountryDTO> getAllCountries() {
        log.info("Fetching all countries from database");
        var countries = countryRepository.findAll();
        return countries.stream()
                .map(CountryDTO::fromEntity)
                .toList();
    }

    /**
     * Retrieves a single country by its ISO 3166-1 alpha-3 code.
     *
     * <p><b>Cache:</b> stored in the {@code countries} cache with the upper-cased
     * country code as the key (e.g. {@code "USA"}, {@code "MEX"}). The cache entry
     * lives until the configured TTL expires or is evicted via
     * {@link #refreshAll()}.</p>
     *
     * @param code the three-letter country code (case-insensitive)
     * @return the matching {@link CountryDTO}
     * @throws ResourceNotFoundException if no country matches the given code
     */
    @Cacheable(cacheNames = "countries", key = "#code.toUpperCase()")
    public CountryDTO getByCode(String code) {
        var upperCode = code.toUpperCase();
        log.info("Fetching country [{}] from database", upperCode);
        return countryRepository.findByCca3(upperCode)
                .map(CountryDTO::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("Country", "cca3", upperCode));
    }

    /**
     * Returns all countries belonging to a given geographic region.
     *
     * <p><b>Cache:</b> stored in the {@code countries:byRegion} cache with the
     * lower-cased region name as the key (e.g. {@code "americas"}, {@code "europe"}).
     * TTL is governed by the {@code countries:byRegion} cache configuration in
     * {@link com.roger.redis.config.RedisConfig}.</p>
     *
     * @param region the region name (e.g. "Americas", "Europe", "Asia")
     * @return a list of {@link CountryDTO}s in the given region; may be empty
     */
    @Cacheable(cacheNames = "countries:byRegion", key = "#region.toLowerCase()")
    public List<CountryDTO> getByRegion(String region) {
        log.info("Fetching countries for region [{}] from database", region);
        var countries = countryRepository.findByRegion(region);
        return countries.stream()
                .map(CountryDTO::fromEntity)
                .toList();
    }

    /**
     * Searches for countries whose common or official name contains the given term
     * (case-insensitive partial match).
     *
     * <p><b>Cache:</b> stored in the {@code countries:search} cache with the
     * lower-cased search term as the key. TTL is governed by the
     * {@code countries:search} cache configuration in
     * {@link com.roger.redis.config.RedisConfig}.</p>
     *
     * @param name the search term to match against country names
     * @return a list of matching {@link CountryDTO}s; may be empty
     */
    @Cacheable(cacheNames = "countries:search", key = "#name.toLowerCase()")
    public List<CountryDTO> searchByName(String name) {
        log.info("Searching countries by name [{}] in database", name);
        var countries = countryRepository.searchByName(name);
        return countries.stream()
                .map(CountryDTO::fromEntity)
                .toList();
    }

    /**
     * Evicts all entries from every country-related cache.
     *
     * <p><b>Cache eviction:</b> clears all entries in the {@code countries},
     * {@code countries:byRegion}, and {@code countries:search} caches. Call this
     * method after bulk data refreshes or administrative updates to ensure stale
     * data is not served from Redis.</p>
     */
    @CacheEvict(cacheNames = {"countries", "countries:byRegion", "countries:search"}, allEntries = true)
    public void refreshAll() {
        log.info("Evicting all country caches");
    }

    /**
     * Returns the total number of countries stored in the database.
     *
     * <p><b>Cache:</b> none — this is a lightweight count query that is always
     * executed against the database to provide an accurate, real-time count.</p>
     *
     * @return the total country count
     */
    public long getCount() {
        return countryRepository.count();
    }
}
