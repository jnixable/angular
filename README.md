# Swed App — Bank Account Management

Monorepo for the homework assignment: a bank account REST API (Java / Spring Boot) and a
single-page application (Angular) that consumes it.

> Scope of this commit: **project structure & configuration only**. The folders, build files,
> Docker setup, and minimal boilerplate are in place. JWT login is implemented; the bank domain
> features (accounts, transactions) are still to be implemented.

## Structure

```
swed-app/
├── backend/            # Spring Boot 3.5 REST API (Java 25, Gradle)
├── frontend/           # Angular 20 SPA (NgRx)
├── docker-compose.yml  # postgres + backend + frontend
└── .env.example        # local dev configuration
```

## Tech stack

| Part     | Choice                                                                         |
| -------- | ------------------------------------------------------------------------------ |
| Backend  | Java 25, Spring Boot 3.5.15, Gradle, Spring Data JPA, Spring Security (JWT)    |
| Database | PostgreSQL, schema managed by Liquibase (Testcontainers for integration tests) |
| Frontend | Angular 20, NgRx (store + effects), served via nginx                           |

## Run everything with Docker

```bash
cp .env.example .env        # optional; sensible defaults are built in
docker compose up --build
```

- Frontend: http://localhost:4200
- Backend: http://localhost:8080 (health: `/actuator/health`, smoke test: `/api/ping`)
- Postgres: localhost:5432 (`bank` / `bank` / db `bankdb`)

## Run services individually

### Database only

```bash
# Start just the PostgreSQL container in the background (no backend/frontend)
docker compose up -d db
# Stop it when you're done
docker compose stop db      # or `docker compose down` to remove the container + network
```

Postgres listens on `localhost:5432` (`bank` / `bank`, db `bankdb`); override the host port with
`DB_PORT` in `.env`. This is the DB the backend's `./gradlew bootRun` expects.

### Backend

```bash
cd backend
# Run against PostgreSQL (expects a DB on localhost:5432 — e.g. `docker compose up -d db`)
./gradlew bootRun
# Local dev with a throwaway PostgreSQL via Testcontainers (requires Docker):
./gradlew bootTestRun        # or run TestBackendApplication from your IDE
# Run tests (Testcontainers starts a real PostgreSQL — requires Docker):
./gradlew test
```

### Frontend

```bash
cd frontend
npm install
npm start        # ng serve on http://localhost:4200, proxies /api -> http://localhost:8080
```

## Authentication

The API uses stateless **JWT bearer** authentication. Exchange credentials for a token, then send
it as `Authorization: Bearer <token>` on protected requests. Tokens are signed with an HMAC-SHA key
(HS384 for the built-in dev secret) and expire **30 minutes** after issue (override via
`APP_JWT_SECRET` / `APP_JWT_EXPIRATION_MS`).

A few demo users are seeded on first startup (the `users` table itself is created by a Liquibase
migration):

| pcode   | password    |
| ------- | ----------- |
| `demo`  | `demo1234`  |
| `alice` | `alice1234` |
| `bob`   | `bob1234`   |

```bash
# 1. Log in to obtain a token
curl -X POST http://localhost:8080/api/user/login \
  -H 'Content-Type: application/json' \
  -d '{"pcode":"demo","password":"demo1234"}'
# -> {"token":"<jwt>","tokenType":"Bearer","expiresInSeconds":1800}

# 2. Call a protected endpoint with the token
curl http://localhost:8080/actuator/info -H "Authorization: Bearer <jwt>"
```

Public endpoints: `POST /api/user/login`, `GET /api/ping`, `GET /actuator/health/**`. Everything
else requires a valid token (missing/invalid token → `401`, request validation errors → `400`).

## Assignment reference

- **Part 1 (backend):** add/debit money, get balance, fixed-rate currency exchange, transaction
  history; multi-account per user; one currency per account; simulated external-logging call before
  debiting; SQL persistence.
- **Part 2 (frontend):** Home (accounts) → Account Overview (history with infinite scroll + balance
  line chart) → Transaction Overview (PDF export). NgRx for state management.

> The credentials and `ddl-auto: update` settings here are for local development only.
