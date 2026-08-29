# HelpCAR Backend

Backend service for **HelpCAR — Ride to Wellness**, a volunteer medical-transport platform
that connects drivers already making a journey with people in rural and remote areas who
need a lift to a medical facility. It also serves an urban carpooling mode.

This repository holds the server. The Flutter client lives in
[helpcar](https://github.com/soumeshmohanty0220/helpcar) and is a thin consumer of the API
defined here — it holds no business logic and no API keys.

> **Status:** early scaffold. The project structure, schema and infrastructure are in place;
> feature modules land in follow-up PRs. See [Roadmap](#roadmap).

---

## Why a separate backend

The original app talked to Firebase directly from the device, which meant matching logic,
credentials and data access all lived in the mobile client. That made four problems
unavoidable:

| Problem | Resolution here |
| --- | --- |
| Matching ran on-device: every user record downloaded, then a line-intersection test per record | Index-backed geospatial pipeline in PostGIS (§6 of the architecture doc) |
| Two competing registration paths — accounts created in the app could never sign in | One `identity` module owning registration, login and tokens |
| Google Maps API key committed in the client and in `google-services.json` | Keys live server-side only; the client never calls Google directly |
| No server-side validation of ride state — the device wrote its own truth | Ride and match lifecycles validated in the service, enforced by DB constraints |

Full design rationale, the matching algorithm and the migration plan:
**[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)**.

---

## Tech stack

| Concern | Choice |
| --- | --- |
| Language / runtime | Java 21 (virtual threads enabled) |
| Framework | Spring Boot 3.5 |
| Build | Gradle (Kotlin DSL) with a version catalog |
| Database | PostgreSQL 16 + **PostGIS 3.4** |
| Migrations | Flyway |
| Realtime | Spring WebSocket (STOMP) + Redis pub/sub |
| Location index | Redis `GEOADD` / `GEOSEARCH` |
| API docs | springdoc-openapi (Swagger UI) |
| Tests | JUnit 5 + Testcontainers (real PostGIS, not a mock) |

PostGIS is a hard requirement, not a convenience: the matching engine is built on
`ST_DWithin`, `ST_Buffer` and `ST_LineLocatePoint` over GiST-indexed `geography` columns.
A plain PostgreSQL instance will fail the first migration.

---

## Quick start

**Prerequisites:** JDK 21, Docker (for Postgres, Redis and the test containers).

```bash
git clone https://github.com/soumeshmohanty0220/helpcar-backend.git
cd helpcar-backend

cp .env.example .env          # defaults already match docker-compose
docker compose up -d          # PostGIS + Redis

SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
```

Verify it is up:

```bash
curl http://localhost:8080/api/v1/ping
# {"status":"ok","service":"helpcar-backend","version":"0.1.0-SNAPSHOT","timestamp":"..."}
```

| What | Where |
| --- | --- |
| Ping (public) | http://localhost:8080/api/v1/ping |
| Health | http://localhost:8080/actuator/health |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |

Run everything, service included, in containers:

```bash
docker compose --profile app up -d --build
```

### Tests

```bash
./gradlew test     # spins up a real PostGIS container via Testcontainers
./gradlew build    # compile + test + assemble
```

Docker must be running — the integration tests boot the actual database so that the
migration and the spatial indexes are exercised for real.

---

## Project layout

Each top-level package under `com.helpcar.backend` is a **bounded context** with its own
domain, and is documented in its `package-info.java`. Modules communicate through explicit
interfaces, so any of them — realistically `matching` or `location` first — can be extracted
into a standalone service later without a rewrite.

```
src/main/java/com/helpcar/backend/
├── HelpcarBackendApplication.java
├── common/                  cross-cutting: security, OpenAPI, error handling
│   ├── config/
│   └── web/
├── identity/                users, roles, credentials, JWT
├── availability/            helper-offered routes (origin → destination, time window)
├── riderequest/             requester trips and their lifecycle
├── matching/                the four-stage matching pipeline
├── location/                live position ingestion + websocket fan-out
└── notification/            push delivery (FCM) and the event log

src/main/resources/
├── application.yml          env-var driven; no secrets committed
├── application-local.yml
└── db/migration/            Flyway — schema is versioned, never hand-edited
```

---

## Configuration

Everything is environment-driven with local-friendly defaults; see
[`.env.example`](.env.example) for the full list.

| Variable | Default | Purpose |
| --- | --- | --- |
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/helpcar` | JDBC URL |
| `DATABASE_USERNAME` / `DATABASE_PASSWORD` | `helpcar` | Credentials |
| `REDIS_HOST` / `REDIS_PORT` | `localhost` / `6379` | Location index and pub/sub |
| `SERVER_PORT` | `8080` | HTTP port |
| `MATCHING_SEARCH_RADIUS_M` | `5000` | Stage 1 candidate radius |
| `MATCHING_MAX_DETOUR_M` | `3000` | Detour budget a helper tolerates |
| `MATCHING_MAX_CANDIDATES` | `100` | Stage 1 hard cap |

No credential belongs in this repository. `.env`, `*.pem`, `*.p12` and
`*-service-account.json` are gitignored.

---

## API surface

Versioned under `/api/v1`. Endpoints marked *planned* are specified in the architecture
doc and land with their module.

| Method | Path | Status |
| --- | --- | --- |
| `GET` | `/api/v1/ping` | ✅ available |
| `POST` | `/api/v1/auth/register` · `/login` · `/refresh` | planned |
| `GET` | `/api/v1/users/me` | planned |
| `POST` · `DELETE` | `/api/v1/helper-routes` | planned |
| `POST` | `/api/v1/ride-requests` | planned |
| `GET` | `/api/v1/ride-requests/{id}/matches` | planned |
| `POST` | `/api/v1/matches/{id}/accept` · `/complete` · `/cancel` | planned |

WebSocket (STOMP) destinations — `/topic/ride/{matchId}/location`,
`/topic/ride/{matchId}/status`, `/user/queue/match-offers` — are described in
[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) §8.

---

## Roadmap

- [x] Project scaffold, schema, infrastructure, CI
- [ ] `identity` — registration, login, JWT, Spring Security filter
- [ ] `availability` — publish and withdraw helper routes
- [ ] `riderequest` — submit trips, lifecycle transitions
- [ ] `matching` — the four-stage pipeline + routing-engine integration
- [ ] `location` — STOMP endpoints, Redis GEO, live tracking
- [ ] `notification` — FCM push
- [ ] Data migration off Firebase (architecture doc §11)

---

## Contributing

Branch from `main`, keep PRs scoped to one module, and make sure `./gradlew build` passes —
CI runs the same command on every pull request.

## License

[MIT](LICENSE)
