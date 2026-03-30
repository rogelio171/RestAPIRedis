package com.roger.redis.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Permissive CORS configuration for PoC development.
 * Allows all origins, methods, and headers.
 *
 * <p>Exposes custom response headers ({@code X-Response-Time-Ms}, {@code X-Total-Count}) so
 * browser clients (e.g. the Next.js dashboard on another origin) can read them — required by the
 * CORS spec for cross-origin XHR/fetch.</p>
 *
 * @author Roger
 */
@Configuration
public class CorsConfig {

    /**
     * CORS configuration source for Spring Security (used when {@code http.cors(Customizer.withDefaults())} is enabled).
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        var configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of(
                "X-Response-Time-Ms",
                "X-Total-Count",
                "X-Total-Pages",
                "X-Current-Page"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
