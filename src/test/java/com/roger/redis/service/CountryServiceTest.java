package com.roger.redis.service;

import com.roger.redis.exception.ResourceNotFoundException;
import com.roger.redis.model.dto.CountryDTO;
import com.roger.redis.model.entity.Country;
import com.roger.redis.repository.CountryRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CountryService}.
 *
 * <p>Uses Mockito to mock the {@link CountryRepository} dependency so that
 * tests exercise only the service-layer logic (DTO mapping, exception handling,
 * delegation to the repository) without requiring a running database or
 * Spring application context.</p>
 *
 * @author Roger
 * @see CountryService
 */
@ExtendWith(MockitoExtension.class)
class CountryServiceTest {

    @Mock
    CountryRepository countryRepository;

    @InjectMocks
    CountryService countryService;

    // ──────────────────────────────────────────────
    // Tests
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("getAllCountries — should return all countries as DTOs")
    void getAllCountries_shouldReturnAllCountriesAsDTOs() {
        // Arrange
        var usa = createTestCountry("USA", "United States", "Americas");
        var mex = createTestCountry("MEX", "Mexico", "Americas");
        when(countryRepository.findAll()).thenReturn(List.of(usa, mex));

        // Act
        var result = countryService.getAllCountries();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).cca3()).isEqualTo("USA");
        assertThat(result.get(0).commonName()).isEqualTo("United States");
        assertThat(result.get(1).cca3()).isEqualTo("MEX");
        assertThat(result.get(1).commonName()).isEqualTo("Mexico");
        verify(countryRepository).findAll();
    }

    @Test
    @DisplayName("getByCode — should return CountryDTO when country is found")
    void getByCode_shouldReturnCountryDTO_whenFound() {
        // Arrange
        var usa = createTestCountry("USA", "United States", "Americas");
        when(countryRepository.findByCca3("USA")).thenReturn(Optional.of(usa));

        // Act
        var result = countryService.getByCode("USA");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.cca3()).isEqualTo("USA");
        assertThat(result.commonName()).isEqualTo("United States");
        assertThat(result.region()).isEqualTo("Americas");
        verify(countryRepository).findByCca3("USA");
    }

    @Test
    @DisplayName("getByCode — should throw ResourceNotFoundException when not found")
    void getByCode_shouldThrowResourceNotFoundException_whenNotFound() {
        // Arrange
        when(countryRepository.findByCca3("XYZ")).thenReturn(Optional.empty());

        // Act & Assert
        var exception = assertThrows(ResourceNotFoundException.class,
                () -> countryService.getByCode("XYZ"));

        assertThat(exception.getMessage()).contains("Country");
        assertThat(exception.getMessage()).contains("XYZ");
        verify(countryRepository).findByCca3("XYZ");
    }

    @Test
    @DisplayName("getByRegion — should return countries belonging to the given region")
    void getByRegion_shouldReturnCountriesByRegion() {
        // Arrange
        var gbr = createTestCountry("GBR", "United Kingdom", "Europe");
        var fra = createTestCountry("FRA", "France", "Europe");
        when(countryRepository.findByRegion("Europe")).thenReturn(List.of(gbr, fra));

        // Act
        var result = countryService.getByRegion("Europe");

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).extracting(CountryDTO::region)
                .containsOnly("Europe");
        assertThat(result).extracting(CountryDTO::cca3)
                .containsExactly("GBR", "FRA");
        verify(countryRepository).findByRegion("Europe");
    }

    @Test
    @DisplayName("searchByName — should return countries matching the search term")
    void searchByName_shouldReturnMatchingCountries() {
        // Arrange
        var usa = createTestCountry("USA", "United States", "Americas");
        var gbr = createTestCountry("GBR", "United Kingdom", "Europe");
        when(countryRepository.searchByName("united")).thenReturn(List.of(usa, gbr));

        // Act
        var result = countryService.searchByName("united");

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).extracting(CountryDTO::commonName)
                .allMatch(name -> name.toLowerCase().contains("united"));
        verify(countryRepository).searchByName("united");
    }

    @Test
    @DisplayName("getCount — should return total country count")
    void getCount_shouldReturnTotalCount() {
        // Arrange
        when(countryRepository.count()).thenReturn(250L);

        // Act
        var result = countryService.getCount();

        // Assert
        assertThat(result).isEqualTo(250L);
        verify(countryRepository).count();
    }

    // ──────────────────────────────────────────────
    // Helper methods
    // ──────────────────────────────────────────────

    /**
     * Creates a {@link Country} entity populated with sensible test defaults.
     *
     * @param cca3       the ISO 3166-1 alpha-3 country code
     * @param commonName the common name of the country
     * @param region     the geographic region
     * @return a fully populated {@code Country} instance
     */
    private Country createTestCountry(String cca3, String commonName, String region) {
        return new Country(
                cca3,
                commonName,
                "Official " + commonName,    // officialName
                commonName + " Capital",     // capital
                region,                      // region
                region + " Sub",             // subregion
                1_000_000L,                  // population
                500_000.0,                   // area
                40.0,                        // latitude
                -100.0,                      // longitude
                38.9,                        // capitalLat
                -77.0,                       // capitalLng
                "https://flags.example.com/" + cca3 + ".png",  // flagUrl
                "https://flags.example.com/" + cca3 + ".svg"   // flagSvg
        );
    }
}
