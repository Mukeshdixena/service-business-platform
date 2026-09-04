# Backend — Service Business Platform

Java 17 / Spring Boot 3.3 backend implementing Phases 1-3 of `CLAUDE_CODE.md`
(Foundation, Services, Appointments) against the contract in
`../docs/API_CONTRACT.md`.

## Prerequisites

- Java 17
- Docker Desktop (for local Postgres via `docker-compose`, and for the
  Testcontainers-backed integration tests)
- No local Maven install required — use the wrapper (`./mvnw` / `mvnw.cmd`)

## Running locally

```bash
# from the repo root: start Postgres (db/user/password: service_platform)
docker compose up -d

# from backend/
./mvnw spring-boot:run
```

The app starts on `http://localhost:8080` with the `local` Spring profile
active by default. CORS is open to `http://localhost:5173` (the Vite dev
server) with credentials enabled.

## Running tests

```bash
./mvnw test
```

This runs:
- Unit tests (slot calculation, overlap arithmetic, the booking state machine)
  with no external dependencies.
- Integration tests (`@SpringBootTest` + MockMvc) against a real PostgreSQL
  instance started via Testcontainers — Docker must be running. Flyway
  migrates the container exactly as it would in any other environment.

If Docker genuinely cannot run in your environment, point the `test` profile's
`DATABASE_URL`/`DATABASE_USERNAME`/`DATABASE_PASSWORD` at a real Postgres
instance instead (see `application-test.yml`) — a Testcontainers-free fallback
was not needed here since Docker was available.

## Environment variables

| Variable | Local default | Required in `prod` |
|---|---|---|
| `DATABASE_URL` | `jdbc:postgresql://localhost:55432/service_platform` | yes, no fallback |
| `DATABASE_USERNAME` | `service_platform` | yes |
| `DATABASE_PASSWORD` | `service_platform` | yes |
| `JWT_SECRET` | insecure dev default | yes, no fallback |
| `CORS_ALLOWED_ORIGINS` | n/a (hardcoded to `http://localhost:5173`) | yes |
| `SPRING_PROFILES_ACTIVE` | `local` | set to `prod` |

Profiles: `application.yml` (common) + `application-local.yml` +
`application-test.yml` + `application-prod.yml`. Schema is Flyway-owned;
`spring.jpa.hibernate.ddl-auto` is `validate` in every profile.

## Notable implementation decisions (see root `ARCHITECTURE.md` for more)

- Entities reference related aggregates by plain UUID foreign-key columns, not
  JPA `@ManyToOne`/`@OneToMany` object graphs — keeps repository queries and
  tenant-scoping explicit and avoids accidental cross-tenant traversal via lazy
  relations.
- The booking creation path takes a Postgres advisory lock
  (`pg_advisory_xact_lock`) scoped to `(businessId, staffId)` before
  re-checking for time-overlap conflicts, so two simultaneous requests for the
  same slot can't both succeed — see `BookingService.acquireSlotLock`.
- Refresh tokens are opaque, server-tracked, revocable tokens (stored as a
  SHA-256 hash), not JWTs, so logout/rotation can actually invalidate them.
- The refresh cookie is `httpOnly; Secure; SameSite=Strict` per the contract.
  Some browsers will not persist a `Secure` cookie delivered over a plain
  `http://localhost` connection — if you hit this in local dev, run the
  frontend dev server over HTTPS or via a proxy that terminates TLS.
