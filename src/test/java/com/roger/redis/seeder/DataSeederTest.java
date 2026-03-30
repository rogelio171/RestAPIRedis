package com.roger.redis.seeder;

import com.roger.redis.model.entity.Country;
import com.roger.redis.repository.CountryRepository;
import com.roger.redis.service.GeoService;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataSeederTest {

    @Test
    void run_shouldSyncGeoIndexFromDatabase_whenDatabaseAlreadySeeded() {
        var countryRepository = mock(CountryRepository.class);
        var geoService = mock(GeoService.class);
        var restClientBuilder = mock(RestClient.Builder.class);
        var seeder = new DataSeeder(countryRepository, geoService, restClientBuilder);

        var usa = new Country("USA", "United States", "United States of America",
                "Washington, D.C.", "Americas", "Northern America",
                331_000_000L, 9_833_520.0, 38.0, -97.0, 38.8951, -77.0364,
                "https://flagcdn.com/w320/us.png", "https://flagcdn.com/us.svg");
        var ata = new Country("ATA", "Antarctica", "Antarctica",
                "", "Antarctic", "",
                0L, 14_000_000.0, -90.0, 0.0, 0.0, 0.0,
                "https://flagcdn.com/w320/aq.png", "https://flagcdn.com/aq.svg");

        when(countryRepository.count()).thenReturn(2L);
        when(countryRepository.findAll()).thenReturn(List.of(usa, ata));

        seeder.run();

        verify(countryRepository).findAll();
        verify(geoService).addCountryLocation("USA", "United States", -97.0, 38.0);
        verify(geoService).addCapitalLocation("USA", -77.0364, 38.8951);
        verify(geoService).addCountryLocation("ATA", "Antarctica", 0.0, -90.0);
        verify(restClientBuilder, never()).build();
    }
}
