# RestAPIRedis dashboard

Next.js UI to exercise the Spring Boot API: JWT login, Redis cache timing (`X-Response-Time-Ms`), cache key inspection, and Redis geospatial endpoints.

## Prerequisites

- Spring Boot API running (default `http://localhost:8080`)
- A user created with `POST /api/v1/auth/register` (or use your seeded user)

## Configuration

Copy `.env.local.example` to `.env.local` and set the API base URL if needed:

```bash
cp .env.local.example .env.local
# edit NEXT_PUBLIC_API_BASE_URL if your API is not on localhost:8080
```

## Run

```bash
npm install
npm run dev
```

Open [http://localhost:3000](http://localhost:3000), sign in, then use:

- **Performance** — `GET /api/v1/countries` with a bar chart of `X-Response-Time-Ms`; evict caches via `POST /api/v1/countries/refresh`
- **Data inspector** — `GET /api/v1/cache/keys` and `GET /api/v1/cache/stats`
- **Geolocation** — map + `GET /api/v1/geo/nearby?lat=&lng=&radiusKm=&limit=`; distance via `GET /api/v1/geo/distance?from=&to=`

## Build

```bash
npm run build
npm start
```
