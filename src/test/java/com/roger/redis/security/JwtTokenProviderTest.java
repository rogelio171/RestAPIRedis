package com.roger.redis.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link JwtTokenProvider} startup validation.
 *
 * @author Roger
 */
class JwtTokenProviderTest {

    @Test
    @DisplayName("constructor should throw when secret is blank")
    void constructor_shouldThrow_whenSecretIsBlank() {
        assertThatThrownBy(() -> new JwtTokenProvider("", 3600000))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("missing");
    }

    @Test
    @DisplayName("constructor should throw when secret is shorter than 32 characters")
    void constructor_shouldThrow_whenSecretIsTooShort() {
        assertThatThrownBy(() -> new JwtTokenProvider("short-secret", 3600000))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 characters");
    }

    @Test
    @DisplayName("constructor should succeed with a valid secret")
    void constructor_shouldSucceed_withValidSecret() {
        var provider = new JwtTokenProvider("this-is-a-valid-secret-that-is-at-least-32-chars", 3600000);

        assertThat(provider.validateToken("not-a-jwt")).isFalse();
    }
}
