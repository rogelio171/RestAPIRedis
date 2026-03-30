package com.roger.redis.controller;

import com.roger.redis.config.TestContainersConfig;
import com.roger.redis.model.dto.AuthRequest;
import com.roger.redis.seeder.DataSeeder;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link AuthController} (JWT mode).
 *
 * <p>Validates Bean Validation on auth endpoints and duplicate-username handling.
 * Uses {@code app.security.mode=jwt} so the AuthController bean is active.
 * A valid JWT_SECRET is provided as a test property.</p>
 *
 * @author Roger
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.security.mode=jwt",
                "app.security.jwt.secret=test-secret-that-is-at-least-32-characters-long"
        }
)
@Import(TestContainersConfig.class)
@AutoConfigureTestRestTemplate
class AuthControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @MockitoBean
    private DataSeeder dataSeeder;

    @Test
    void register_shouldReturn400_whenUsernameIsBlank() {
        var request = new AuthRequest("", "validpassword");
        var response = restTemplate.postForEntity("/api/v1/auth/register", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("message").toString()).containsIgnoringCase("username");
    }

    @Test
    void register_shouldReturn400_whenPasswordIsBlank() {
        var request = new AuthRequest("validuser", "");
        var response = restTemplate.postForEntity("/api/v1/auth/register", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("message").toString()).containsIgnoringCase("password");
    }

    @Test
    void register_shouldReturn201_thenConflictOnDuplicate() {
        var request = new AuthRequest("uniqueuser", "password123");

        var first = restTemplate.postForEntity("/api/v1/auth/register", request, Void.class);
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        var second = restTemplate.postForEntity("/api/v1/auth/register", request, Map.class);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(second.getBody()).isNotNull();
        assertThat(second.getBody().get("message").toString()).containsIgnoringCase("already exists");
    }
}
