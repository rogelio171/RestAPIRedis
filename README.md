# RestAPIRedis

A Proof of Concept (PoC) Spring Boot REST API that demonstrates Redis as a caching layer and geospatial engine alongside PostgreSQL for persistence. The application seeds country data from the [REST Countries API](https://restcountries.com/) and exposes endpoints for querying, searching, and performing geospatial operations — all backed by Redis caching for improved performance.

## Tech Stack

| Component            | Technology                                        |
|----------------------|---------------------------------------------------|
| Language             | Java 25                                           |
| Framework            | Spring Boot 4.1.0-SNAPSHOT                        |
| Build Tool           | Maven (via Maven Wrapper `./mvnw`)                |
| Primary Database     | PostgreSQL 17                                     |
| Cache / Geo Engine   | Redis 8 (via Spring Data Redis)                   |
| Serialization        | JSON (Spring Cache values) + Kryo (`RedisTemplate`) |
| Monitoring           | Spring Boot Actuator                              |
| Containerization     | Docker Compose (auto-managed by Spring Boot)      |
| Testing              | JUnit 5, Mockito, Testcontainers 1.21.4           |

## Prerequisites

- **Java 25** — ensure `JAVA_HOME` is set
- **Docker** — required for Redis and PostgreSQL containers

## Quick Start

```bash
# Clone the repository
git clone <repo-url> && cd RestAPIRedis

# Run the application (Docker containers start automatically)
./mvnw spring-boot:run

# Run the test suite
./mvnw test
```

> Spring Boot Docker Compose integration automatically starts Redis and PostgreSQL containers defined in `compose.yaml` when the application starts. No manual `docker compose up` is needed.

## Build Commands

| Action              | Command                            |
|---------------------|------------------------------------|
| Build               | `./mvnw clean package`             |
| Run (dev)           | `./mvnw spring-boot:run`           |
| Run tests           | `./mvnw test`                      |
| Skip tests on build | `./mvnw clean package -DskipTests` |

## REST API Endpoints

### Countries (`/api/v1/countries`)

| Method | Path                                | Description                 | Parameters                | Response Headers                        |
|--------|-------------------------------------|-----------------------------|---------------------------|-----------------------------------------|
| GET    | `/api/v1/countries`                 | List all countries          | —                         | `X-Response-Time-Ms`, `X-Total-Count`   |
| GET    | `/api/v1/countries/{code}`          | Get country by ISO alpha-3  | `code` (path)             | `X-Response-Time-Ms`                    |
| GET    | `/api/v1/countries/region/{region}` | Get countries by region     | `region` (path)           | `X-Response-Time-Ms`, `X-Total-Count`   |
| GET    | `/api/v1/countries/search`          | Search by name              | `name` (query)            | `X-Response-Time-Ms`, `X-Total-Count`   |
| GET    | `/api/v1/countries/count`           | Get total country count     | —                         | —                                       |
| POST   | `/api/v1/countries/refresh`         | Evict all country caches    | —                         | —                                       |

### Geospatial (`/api/v1/geo`)

| Method | Path                      | Description                     | Parameters                                                                 |
|--------|---------------------------|---------------------------------|----------------------------------------------------------------------------|
| GET    | `/api/v1/geo/nearby`      | Find countries within radius    | `lat`, `lng` (required), `radiusKm` (default: 1000), `limit` (default: 10)|
| GET    | `/api/v1/geo/distance`    | Distance between two countries  | `from`, `to` (query, ISO alpha-3 codes)                                    |
| GET    | `/api/v1/geo/position/{code}` | Get coordinates of a country | `code` (path)                                                              |
| GET    | `/api/v1/geo/stats`       | Geospatial index statistics     | —                                                                          |

### Cache Management (`/api/v1/cache`)

| Method | Path                         | Description                          |
|--------|------------------------------|--------------------------------------|
| GET    | `/api/v1/cache/keys`         | List all cache keys by cache name    |
| GET    | `/api/v1/cache/keys/{name}`  | List keys for a specific cache       |
| GET    | `/api/v1/cache/stats`        | Cache statistics and Redis memory    |
| DELETE | `/api/v1/cache/{name}`       | Evict a specific cache               |
| DELETE | `/api/v1/cache`              | Evict all caches                     |

### Actuator

| Path                       | Description         |
|----------------------------|---------------------|
| `/actuator/health`         | Health check        |
| `/actuator/info`           | Application info    |
| `/actuator/metrics`        | Application metrics |
| `/actuator/caches`         | Cache details       |

## Redis Features Demonstrated

### Caching (Spring Cache Abstraction)

The application uses `@Cacheable` and `@CacheEvict` annotations with per-cache TTL policies:

| Cache Name              | TTL        | Description                              |
|-------------------------|------------|------------------------------------------|
| `countries`             | 30 minutes | All countries list                       |
| `countries:byRegion`    | 20 minutes | Countries filtered by geographic region  |
| `countries:search`      | 5 minutes  | Name search results                      |
| Default                 | 10 minutes | Any other cached data                    |

Spring **@Cacheable** values are stored as **JSON** (`GenericJackson2JsonRedisSerializer`) so HTTP responses and Spring DevTools reloads do not hit Kryo/classloader issues. **`RedisTemplate`** still uses **Kryo** for compact binary values where applicable. Cache keys use the `restapi-json-v2::` prefix (see `RedisConfig.CACHE_KEY_NAMESPACE`).

### Geospatial Operations

Redis geospatial commands power the proximity and distance features:

| Redis Command | Usage                                              |
|---------------|----------------------------------------------------|
| `GEOADD`      | Store country centroid and capital city coordinates |
| `GEORADIUS`   | Find countries within a radius from a point         |
| `GEODIST`     | Calculate great-circle distance between countries   |
| `GEOPOS`      | Retrieve stored coordinates for a country           |
| `ZCARD`       | Count entries in the geospatial index               |

Redis keys used:
- `geo:countries` — Country centroid geospatial index
- `geo:capitals` — Capital city geospatial index
- `geo:names` — Hash mapping country codes to display names

### Cache Introspection

The Cache Management endpoints use `SCAN`-based key discovery (production-safe, avoids blocking `KEYS` command) and expose Redis `INFO memory` statistics.

## Architecture

```
Request → Controller → Service (cache check) → Repository → PostgreSQL
                          ↓ (cache hit)
                        Redis → Response
```

### Project Structure

```
src/main/java/com/roger/redis/
├── RestApiRedisApplication.java       # Application entry point
├── config/
│   └── RedisConfig.java               # Redis cache manager and template configuration
├── controller/
│   ├── CacheController.java           # Cache introspection and eviction endpoints
│   ├── CountryController.java         # Country CRUD and search endpoints
│   └── GeoController.java            # Geospatial query endpoints
├── exception/
│   ├── GlobalExceptionHandler.java    # @RestControllerAdvice for centralized errors
│   └── ResourceNotFoundException.java # Custom 404 exception
├── model/
│   ├── dto/
│   │   ├── CountryDTO.java            # Country response DTO (record)
│   │   ├── ErrorResponse.java         # Structured error response (record)
│   │   └── GeoSearchResult.java       # Geospatial search result (record)
│   └── entity/
│       └── Country.java               # JPA entity for country data
├── repository/
│   └── CountryRepository.java         # Spring Data JPA repository with custom queries
├── seeder/
│   └── DataSeeder.java                # Startup data loader from REST Countries API
├── serializer/
│   └── KryoRedisSerializer.java       # Thread-safe Kryo serializer for Redis
└── service/
    ├── CacheService.java              # Cache management operations
    ├── CountryService.java            # Country business logic with caching
    └── GeoService.java               # Redis geospatial operations
```

### Key Design Decisions

- **Constructor injection** throughout — no field injection
- **DTO pattern** — entities are never exposed directly in API responses
- **Cache-aside pattern** — via Spring Cache annotations
- **Redis serialization** — JSON for Spring Cache entries; Kryo for `RedisTemplate` (thread-local Kryo, pre-registered collection types)
- **Global exception handling** — structured error responses with status, message, timestamp, and path
- **Idempotent seeding** — `DataSeeder` skips if the database is already populated

## Data Seeding

On first startup, the application automatically:

1. Fetches all country data from the [REST Countries API](https://restcountries.com/v3.1/all)
2. Persists ~250 countries to PostgreSQL
3. Loads country centroid and capital coordinates into Redis geospatial indices

Subsequent restarts skip seeding if data already exists.

## Infrastructure

### Docker Compose (`compose.yaml`)

```yaml
services:
  redis:
    image: 'redis:8'
    ports:
      - '6379'
  postgres:
    image: 'postgres:17'
    environment:
      POSTGRES_DB: countries_db
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - '5432'
```

Spring Boot Docker Compose integration manages container lifecycle automatically — containers start with the app and use dynamic port mapping.

### Configuration (`application.properties`)

```properties
spring.application.name=RestAPIRedis

# JPA / Hibernate
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.open-in-view=false

# Redis Caching
spring.cache.type=redis
spring.cache.redis.key-prefix=restapi-json-v2::
spring.cache.redis.use-key-prefix=true
spring.cache.redis.time-to-live=600000

# Actuator
management.endpoints.web.exposure.include=health,info,metrics,caches
```

## Testing

The project includes **22 tests** across unit and integration layers:

### Unit Tests (Mockito)

| Test Class         | Tests | Coverage                                    |
|--------------------|-------|---------------------------------------------|
| `CountryServiceTest` | 6   | DTO mapping, exception handling, delegation |
| `GeoServiceTest`     | 3   | GEOADD, GEODIST, ZCARD via mocked template |

### Integration Tests (Testcontainers)

| Test Class                          | Tests | Coverage                                          |
|-------------------------------------|-------|---------------------------------------------------|
| `RestApiRedisApplicationTests`      | 1     | Spring context loads successfully                 |
| `CountryControllerIntegrationTest`  | 6     | CRUD, search, region filter, cache eviction       |
| `CacheControllerIntegrationTest`    | 3     | Key listing, stats, full eviction                 |
| `GeoControllerIntegrationTest`      | 3     | Nearby search, distance calculation, position     |

Integration tests run against real PostgreSQL and Redis instances via [Testcontainers](https://testcontainers.com/) — no external infrastructure required.

```bash
./mvnw test
```

## Dependencies

### Runtime

| Dependency                           | Version            | Purpose                                    |
|--------------------------------------|--------------------|--------------------------------------------|
| `spring-boot-starter-webmvc`        | 4.1.0-SNAPSHOT     | REST API with Spring MVC                   |
| `spring-boot-starter-data-jpa`      | 4.1.0-SNAPSHOT     | JPA / Hibernate for PostgreSQL             |
| `spring-boot-starter-data-redis`    | 4.1.0-SNAPSHOT     | Redis data access and caching              |
| `spring-boot-starter-restclient`    | 4.1.0-SNAPSHOT     | REST client for external API calls         |
| `spring-boot-starter-json`          | 4.1.0-SNAPSHOT     | Jackson JSON serialization (Jackson 3.x)   |
| `spring-boot-starter-actuator`      | 4.1.0-SNAPSHOT     | Health checks, metrics, monitoring         |
| `spring-boot-devtools`              | 4.1.0-SNAPSHOT     | Hot reload and dev-time conveniences       |
| `spring-boot-docker-compose`        | 4.1.0-SNAPSHOT     | Auto-manage Docker Compose on startup      |
| `postgresql`                        | Managed by BOM     | PostgreSQL JDBC driver                     |
| `kryo`                              | 5.6.2              | Binary serialization for Redis values      |

### Test

| Dependency                           | Version        | Purpose                              |
|--------------------------------------|----------------|--------------------------------------|
| `spring-boot-starter-test`          | 4.1.0-SNAPSHOT | JUnit 5, Mockito, AssertJ            |
| `spring-boot-starter-webmvc-test`   | 4.1.0-SNAPSHOT | MockMvc and web layer testing        |
| `spring-boot-starter-data-redis-test` | 4.1.0-SNAPSHOT | Redis integration test support     |
| `spring-boot-starter-actuator-test` | 4.1.0-SNAPSHOT | Actuator endpoint testing            |
| `spring-boot-testcontainers`        | 4.1.0-SNAPSHOT | Testcontainers Spring Boot support   |
| `testcontainers:junit-jupiter`      | 1.21.4         | JUnit 5 container lifecycle          |
| `testcontainers:postgresql`         | 1.21.4         | PostgreSQL container for tests       |
| `testcontainers-redis`              | 2.2.4          | Redis container for tests            |

## API Usage Examples

```bash
# Get all countries
curl http://localhost:8080/api/v1/countries

# Get a specific country
curl http://localhost:8080/api/v1/countries/USA

# Search by name
curl "http://localhost:8080/api/v1/countries/search?name=united"

# Get countries in a region
curl http://localhost:8080/api/v1/countries/region/Americas

# Find countries within 2000 km of coordinates
curl "http://localhost:8080/api/v1/geo/nearby?lat=38&lng=-97&radiusKm=2000&limit=5"

# Distance between two countries
curl "http://localhost:8080/api/v1/geo/distance?from=USA&to=MEX"

# Get geospatial position
curl http://localhost:8080/api/v1/geo/position/USA

# View cache statistics
curl http://localhost:8080/api/v1/cache/stats

# Evict all caches
curl -X DELETE http://localhost:8080/api/v1/cache

# Health check
curl http://localhost:8080/actuator/health
```

## License

This project is a Proof of Concept for learning and demonstration purposes.
