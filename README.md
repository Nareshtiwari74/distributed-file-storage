# Distributed File Storage System

A fault-tolerant file storage backend built with Spring Boot and Java 21 — a small, understandable take on what systems like S3 or Google Drive do under the hood: store files reliably so that no single machine failure loses data or makes it unavailable.

File **contents** are stored across distributed storage nodes with replication; file **metadata** lives in PostgreSQL. The system is built as a **modular monolith**, phase by phase, with tests and documentation at each step.

> **Status:** Phases 1–2 complete (foundation + authentication). File storage, multi-node replication, failover and self-healing are in progress — see the roadmap.

## Tech stack

- **Language:** Java 21
- **Framework:** Spring Boot 3.3.5 (Spring MVC, Spring Data JPA, Spring Security)
- **Auth:** JWT (jjwt), BCrypt password hashing
- **Database:** PostgreSQL 16, schema managed with Flyway migrations
- **Docs:** OpenAPI / Swagger UI
- **Testing:** JUnit 5, Mockito, Testcontainers
- **Build & infra:** Maven, Docker, Docker Compose

## What works today

**Phase 1 — Foundation**
- Health endpoint (`/api/health`) that verifies real database connectivity
- Global exception handling with a uniform error response
- Structured logging with a per-request correlation ID
- Environment-driven configuration (no hardcoded secrets)
- One-command startup via Docker Compose

**Phase 2 — Authentication**
- User registration and login
- JWT-based stateless authentication
- BCrypt password hashing (plaintext never stored)
- Route protection: public paths open, everything else requires a valid token
- Correct status codes: 201 on register, 401 unauthenticated, 409 duplicate email

## API endpoints

| Method | Path                | Auth | Description                       |
|--------|---------------------|------|-----------------------------------|
| GET    | /api/health         | No   | App + database health             |
| POST   | /api/auth/register  | No   | Create an account, returns a JWT  |
| POST   | /api/auth/login     | No   | Verify credentials, returns a JWT |

Interactive docs: `http://localhost:8080/swagger-ui.html`

## Getting started — run it on your machine

### Prerequisites

- **JDK 21** — check with `java -version`
- **Maven 3.9+** — check with `mvn -version` (macOS: `brew install maven`)
- **Docker Desktop** — must be running

### 1. Clone the repository

```bash
git clone https://github.com/Nareshtiwari74/distributed-file-storage.git
cd distributed-file-storage
```

### 2. Start PostgreSQL

```bash
docker compose up -d postgres
```

Default credentials (database `dfs`, user `dfs`, password `dfs`) come from built-in defaults — no setup needed.

### 3. Run the application

```bash
mvn spring-boot:run
```

Wait for `Started DistributedFileStorageApplication`. The API is live at `http://localhost:8080`.

### 4. Verify it works

```bash
curl http://localhost:8080/api/health
```

Expected: `{"status":"UP","components":{"database":"UP"}}`

Open the docs: `http://localhost:8080/swagger-ui.html`

### 5. Try the auth flow

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"you@example.com","password":"password123"}'

curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"you@example.com","password":"password123"}'
```

Both return a JWT. Send it as `Authorization: Bearer <token>` on protected routes.

### Run everything in Docker (optional)

```bash
docker compose up --build
```

### Stopping

```bash
docker compose down       # stop PostgreSQL
docker compose down -v    # also wipe the database volume
```

### Troubleshooting

- **`mvn: command not found`** — install Maven (`brew install maven`).
- **`Cannot connect to the Docker daemon`** — start Docker Desktop and wait until running.
- **`password authentication failed for user "dfs"`** — stale DB volume. Reset: `docker compose down -v && docker compose up -d postgres`.
- **`Port 8080 already in use`** — stop it: `kill -9 $(lsof -t -i :8080)`.

## Testing

```bash
mvn test      # unit and web-layer tests
mvn verify    # also runs integration tests (Testcontainers; requires Docker)
```

## Architecture

Modular monolith — one deployable, split internally by feature package (`auth`, `health`, `config`, `common`, `user`). Distribution lives in the storage layer (planned), not in splitting the app into microservices.

Request flow: