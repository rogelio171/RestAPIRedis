package com.roger.redis.service;

import com.roger.redis.exception.ResourceNotFoundException;
import com.roger.redis.model.dto.CountryDTO;
import com.roger.redis.model.entity.Country;
import com.roger.redis.repository.CountryRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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
 *   <li><b>{@code countries}</b> — caches individual country lookups (by code), paginated
 *       results, and the full country list. TTL is governed by the {@code countries} cache
 *       configuration defined in {@link com.roger.redis.config.RedisConfig}.</li>
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

    public CountryService(CountryRepository countryRepository) {
        this.countryRepository = countryRepository;
    }

    /**
     * Returns a paginated list of countries.
     *
     * <p><b>Cache:</b> stored in the {@code countries} cache under a composite key
     * of page index and size (e.g. {@code "page:0:50"}).</p>
     *
     * @param page the zero-based page index
     * @param size the number of items per page
     * @return a {@link Page} of {@link CountryDTO}s
     */
    @Transactional(readOnly = true)
    public Page<CountryDTO> getAllCountriesPaginated(int page, int size) {
        log.info("Fetching countries page {} (size {}) from database", page, size);
        return countryRepository.findAll(PageRequest.of(page, size))
                .map(CountryDTO::fromEntity);
    }

    /**
     * Returns every country in the database as a list of DTOs.
     *
     * <p><b>Cache:</b> stored in the {@code countries} cache under the fixed key
     * {@code "all"}.</p>
     *
     * @return an unmodifiable list of all {@link CountryDTO}s
     */
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "countries", key = "'all'")
    public List<CountryDTO> getAllCountries() {
        log.info("Fetching all countries from database");
        var countries = countryRepository.findAll();
        return countries.stream()
                .map(CountryDTO::fromEntity)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    /**
     * Retrieves a single country by its ISO 3166-1 alpha-3 code.
     *
     * @param code the three-letter country code (case-insensitive)
     * @return the matching {@link CountryDTO}
     * @throws ResourceNotFoundException if no country matches the given code
     */
    @Transactional(readOnly = true)
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
     * @param region the region name (e.g. "Americas", "Europe", "Asia")
     * @return a list of {@link CountryDTO}s in the given region; may be empty
     */
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "countries:byRegion", key = "#region.toLowerCase()")
    public List<CountryDTO> getByRegion(String region) {
        log.info("Fetching countries for region [{}] from database", region);
        var countries = countryRepository.findByRegion(region);
        return countries.stream()
                .map(CountryDTO::fromEntity)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    /**
     * Searches for countries whose common or official name contains the given term
     * (case-insensitive partial match).
     *
     * @param name the search term to match against country names
     * @return a list of matching {@link CountryDTO}s; may be empty
     */
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "countries:search", key = "#name.toLowerCase()")
    public List<CountryDTO> searchByName(String name) {
        log.info("Searching countries by name [{}] in database", name);
        var countries = countryRepository.searchByName(name);
        return countries.stream()
                .map(CountryDTO::fromEntity)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    /**
     * Evicts all entries from every country-related cache.
     */
    @Transactional
    @CacheEvict(cacheNames = {"countries", "countries:byRegion", "countries:search"}, allEntries = true)
    public void refreshAll() {
        log.info("Evicting all country caches");
    }

    /**
     * Returns the total number of countries stored in the database.
     *
     * @return the total country count
     */
    @Transactional(readOnly = true)
    public long getCount() {
        return countryRepository.count();
    }
}
