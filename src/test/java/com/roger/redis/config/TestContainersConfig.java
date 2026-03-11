package com.roger.redis.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Testcontainers configuration that provides PostgreSQL and Redis containers
 * for integration tests.
 *
 * <p>Spring Boot's {@link ServiceConnection} annotation automatically configures
 * the datasource and Redis connection properties from the running containers,
 * eliminating the need for manual property overrides in test profiles.</p>
 *
 * @author Roger
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestContainersConfig {

    /**
     * Creates a PostgreSQL Testcontainer with the {@code postgres:17} image.
     *
     * @return a configured {@link PostgreSQLContainer} instance
     */
    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>("postgres:17");
    }

    /**
     * Creates a Redis Testcontainer with the {@code redis:8} image, exposing port 6379.
     *
     * @return a configured {@link GenericContainer} instance acting as a Redis server
     */
    @Bean
    @ServiceConnection(name = "redis")
    GenericContainer<?> redisContainer() {
        return new GenericContainer<>("redis:8").withExposedPorts(6379);
    }
}
