package com.roger.redis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Entry point for the RestAPIRedis Proof of Concept.
 * <p>
 * Demonstrates PostgreSQL persistence improved with Redis caching,
 * geospatial queries, and Kryo-based serialization.
 */
@SpringBootApplication
@EnableCaching
public class RestApiRedisApplication {

	static void main(String[] args) {
		SpringApplication.run(RestApiRedisApplication.class, args);
	}

}
