package com.roger.redis.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * Generates and validates self-issued JWTs using HMAC-SHA256.
 *
 * <p>Reads {@code app.security.jwt.secret} and {@code app.security.jwt.expiration-ms} from configuration.
 * The secret must be at least 256 bits (32 characters) for HS256.</p>
 *
 * @author Roger
 */
@Component
@ConditionalOnProperty(name = "app.security.mode", havingValue = "jwt")
public class JwtTokenProvider {

    private static final JWSAlgorithm ALGORITHM = JWSAlgorithm.HS256;
    private static final String CLAIM_AUTHORITIES = "authorities";

    private final String secret;
    private final long expirationMs;

    public JwtTokenProvider(
            @Value("${app.security.jwt.secret}") String secret,
            @Value("${app.security.jwt.expiration-ms:3600000}") long expirationMs) {
        this.secret = secret;
        this.expirationMs = expirationMs;

        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT secret is missing. Set the JWT_SECRET environment variable.");
        }
        if (secret.length() < 32) {
            throw new IllegalStateException(
                    "JWT secret must be at least 32 characters for HS256. Current length: " + secret.length());
        }
    }

    /**
     * Generates a JWT for the given authentication.
     *
     * @param authentication the authenticated user
     * @return the signed JWT string
     */
    public String generateToken(Authentication authentication) {
        try {
            String username = authentication.getName();
            List<String> authorities = authentication.getAuthorities().stream()
                    .map(Object::toString)
                    .toList();

            Instant now = Instant.now();
            Instant expiry = now.plusMillis(expirationMs);

            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(username)
                    .claim(CLAIM_AUTHORITIES, authorities)
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(expiry))
                    .build();

            SignedJWT signedJWT = new SignedJWT(new JWSHeader(ALGORITHM), claims);
            byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
            signedJWT.sign(new MACSigner(secretBytes));

            return signedJWT.serialize();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate JWT", e);
        }
    }

    /**
     * Extracts the username (subject) from the JWT.
     *
     * @param token the JWT string
     * @return the username
     */
    public String getUsernameFromToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
            if (!signedJWT.verify(new MACVerifier(secretBytes))) {
                return null;
            }
            return signedJWT.getJWTClaimsSet().getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Validates the JWT signature and expiration.
     *
     * @param token the JWT string
     * @return true if valid
     */
    public boolean validateToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
            if (!signedJWT.verify(new MACVerifier(secretBytes))) {
                return false;
            }
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            if (claims.getExpirationTime() == null || claims.getExpirationTime().before(new Date())) {
                return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns the token expiration time in seconds (for the response).
     *
     * @return expiration in seconds
     */
    public long getExpirationSeconds() {
        return expirationMs / 1000;
    }
}
