package com.roger.redis.controller;

import com.roger.redis.model.dto.AuthRequest;
import com.roger.redis.model.dto.AuthResponse;
import com.roger.redis.model.entity.AppUser;
import com.roger.redis.repository.UserRepository;
import com.roger.redis.security.JwtTokenProvider;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication endpoints for JWT mode: register and login.
 * Active only when {@code app.security.mode=jwt}.
 *
 * @author Roger
 */
@RestController
@RequestMapping("/api/v1/auth")
@ConditionalOnProperty(name = "app.security.mode", havingValue = "jwt")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * Registers a new user.
     *
     * @param request username and password
     * @return 201 on success, 400 if username already exists
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest request) {
        if (request.username() == null || request.username().isBlank()
                || request.password() == null || request.password().isBlank()) {
            return ResponseEntity.badRequest().body("Username and password are required");
        }
        if (userRepository.existsByUsername(request.username())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Username already exists");
        }
        String encodedPassword = passwordEncoder.encode(request.password());
        AppUser user = new AppUser(request.username(), encodedPassword, AppUser.DEFAULT_ROLE);
        userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Authenticates the user and returns a JWT.
     *
     * @param request username and password
     * @return 200 with token and expiresIn, or 401 if invalid credentials
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        String token = jwtTokenProvider.generateToken(authentication);
        long expiresIn = jwtTokenProvider.getExpirationSeconds();
        return ResponseEntity.ok(new AuthResponse(token, expiresIn));
    }
}
