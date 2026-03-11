# AGENTS.md — RestAPIRedis

## Project Overview

**RestAPIRedis** is a Proof of Concept (PoC) Spring Boot REST API that leverages Redis as a caching layer to improve performance. The application exposes RESTful endpoints and uses Redis for fast data retrieval and caching strategies.

---

## Tech Stack

| Component          | Technology                          |
|--------------------|-------------------------------------|
| Language           | Java 25                             |
| Framework          | Spring Boot 4.1.0-SNAPSHOT          |
| Build Tool         | Maven (with Maven Wrapper `./mvnw`) |
| Cache / Data Store | Redis (via Spring Data Redis)       |
| Web Layer          | Spring Web MVC                      |
| Monitoring         | Spring Boot Actuator                |
| Dev Tools          | Spring Boot DevTools, Docker Compose Integration |
| Containerization   | Docker Compose (`compose.yaml`)     |
| Testing            | JUnit 5 (Spring Boot Test)          |

---

## Project Structure

```
RestAPIRedis/
├── compose.yaml                           # Docker Compose for Redis
├── pom.xml                                # Maven build configuration
├── mvnw / mvnw.cmd                        # Maven Wrapper scripts
├── src/
│   ├── main/
│   │   ├── java/com/roger/redis/          # Base package
│   │   │   └── RestApiRedisApplication.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── java/com/roger/redis/
│           └── RestApiRedisApplicationTests.java
└── AGENTS.md
```

### Recommended Package Layout

When adding new code, follow this package convention under `com.roger.redis`:

```
com.roger.redis
├── config/          # Configuration classes (Redis, CORS, etc.)
├── controller/      # REST controllers
├── service/         # Business logic / service layer
├── repository/      # Data access / Redis repositories
├── model/           # Domain entities and DTOs
│   ├── entity/      # Redis hash / domain objects
│   └── dto/         # Request/Response DTOs
└── exception/       # Custom exceptions and global error handling
```

---

## Build & Run

### Prerequisites

- **Java 25** (ensure `JAVA_HOME` is set)
- **Docker** (for Redis container via Docker Compose)

### Commands

| Action                | Command                    |
|-----------------------|----------------------------|
| Build                 | `./mvnw clean package`     |
| Run (dev)             | `./mvnw spring-boot:run`   |
| Run tests             | `./mvnw test`              |
| Skip tests on build   | `./mvnw clean package -DskipTests` |

> **Note:** Spring Boot Docker Compose integration (`spring-boot-docker-compose`) automatically starts the Redis container defined in `compose.yaml` when the application starts in development mode. No manual `docker compose up` is needed during development.

---

## Infrastructure

### Redis (Docker Compose)

The `compose.yaml` defines a Redis service:

- **Image**: `redis:latest`
- **Port**: `6379` (mapped dynamically by Docker Compose integration)

Spring Boot auto-configures the Redis connection through Docker Compose service connection. No manual `spring.data.redis.*` properties are needed when using Docker Compose integration.

### Application Properties

Configuration lives in `src/main/resources/application.properties`. Currently only sets the application name. Future configuration should be added here (e.g., caching TTL, custom Redis settings, actuator exposure).

---

## Coding Conventions

### General

- Use **Java 25** features where appropriate (records, pattern matching, sealed classes, etc.)
- Follow standard **Spring Boot conventions** for layered architecture
- Use **constructor injection** (not field injection) for dependency injection
- Keep controllers thin — delegate business logic to the service layer
- Use `@RestController` and `@RequestMapping` for REST endpoints
- Return proper HTTP status codes using `ResponseEntity`

### Redis & Caching

- Use **Spring Data Redis** (`@RedisHash`, `CrudRepository`) for Redis-backed entities
- For caching, use Spring's `@Cacheable`, `@CacheEvict`, `@CachePut` annotations
- Configure cache manager and Redis template in a dedicated `@Configuration` class under `config/`
- Define serialization strategy (prefer JSON over JDK serialization for Redis values)

### REST API Design

- Use plural nouns for resource endpoints (e.g., `/api/products`, `/api/users`)
- Version APIs if needed (e.g., `/api/v1/...`)
- Use standard HTTP methods: `GET`, `POST`, `PUT`, `DELETE`, `PATCH`
- Return consistent response bodies with appropriate status codes
- Implement global exception handling with `@RestControllerAdvice`

### Error Handling

- Create custom exception classes under `exception/` package
- Use `@RestControllerAdvice` with `@ExceptionHandler` for centralized error responses
- Return structured error responses (status, message, timestamp, path)

### Testing

- Write unit tests for service layer logic
- Write integration tests for controllers using `@WebMvcTest` or `@SpringBootTest`
- Use Spring Data Redis test support for Redis integration tests
- Test class names should follow `*Test.java` (unit) or `*IntegrationTest.java` (integration) convention
- Use Actuator test support for health/metrics endpoint verification

### Documentation

- Add Javadoc to public classes and methods
- Keep `application.properties` documented with comments for non-obvious settings

---

## Dependencies Reference

### Runtime

| Dependency                        | Purpose                                      |
|-----------------------------------|----------------------------------------------|
| `spring-boot-starter-webmvc`     | REST API with Spring MVC                     |
| `spring-boot-starter-data-redis` | Redis data access and caching                |
| `spring-boot-starter-actuator`   | Health checks, metrics, monitoring endpoints |
| `spring-boot-devtools`           | Hot reload and dev-time conveniences         |
| `spring-boot-docker-compose`     | Auto-manage Docker Compose services on startup |

### Test

| Dependency                              | Purpose                         |
|-----------------------------------------|---------------------------------|
| `spring-boot-starter-webmvc-test`       | MockMvc and web layer testing   |
| `spring-boot-starter-data-redis-test`   | Redis integration test support  |
| `spring-boot-starter-actuator-test`     | Actuator endpoint testing       |

---

## Key Reminders

1. **Do not hardcode Redis connection details** — rely on Docker Compose integration or `application.properties` profiles.
2. **Always use the Maven Wrapper** (`./mvnw`) instead of a globally installed Maven.
3. **Redis starts automatically** in dev mode via Spring Boot Docker Compose support — no need to run `docker compose up` manually.
4. **Actuator endpoints** are available for monitoring — configure exposure in `application.properties` as needed (e.g., `management.endpoints.web.exposure.include=health,info,metrics`).
5. **This is a PoC** — prioritize clean, demonstrable code over production-grade infrastructure concerns.
