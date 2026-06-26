# Swed App — Bank Account Management


## Structure

```
swed-app/
├── backend/            # Spring Boot 3.5 REST API (Java 25, Gradle)
├── frontend/           # Angular 20 SPA (NgRx)
├── docker-compose.yml  # postgres + backend + frontend
└── .env.example        # local dev configuration
```

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


### Backend

```bash
cd backend
# Run against PostgreSQL (expects a DB on localhost:5432 — e.g. `docker compose up -d db`)
./gradlew bootRun
```

### Frontend

```bash
cd frontend
npm install
npm start        # ng serve on http://localhost:4200, proxies /api -> http://localhost:8080
```

