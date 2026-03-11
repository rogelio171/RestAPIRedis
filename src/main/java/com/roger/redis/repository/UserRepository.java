package com.roger.redis.repository;

import com.roger.redis.model.entity.AppUser;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link AppUser} entities.
 *
 * @author Roger
 */
public interface UserRepository extends JpaRepository<AppUser, Long> {

    /**
     * Finds a user by username.
     *
     * @param username the username
     * @return an optional containing the user, or empty if not found
     */
    Optional<AppUser> findByUsername(String username);

    /**
     * Returns whether a user exists with the given username.
     *
     * @param username the username
     * @return true if a user exists
     */
    boolean existsByUsername(String username);
}
