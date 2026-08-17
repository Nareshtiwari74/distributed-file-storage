# Distributed File Storage System

A fault-tolerant file storage backend built with **Spring Boot** and **Java 21** — a small, understandable take on what systems like Amazon S3 or Google Drive do under the hood: store files reliably so that no single machine failure loses data or makes it unavailable.

File **contents** are stored in object storage (MinIO); file **metadata** lives in PostgreSQL. The system is built as a **modular monolith**, phase by phase, with automated tests and documentation at each step.

> **Status:** Phases 1–3 complete (foundation, authentication, file storage). Multi-node replication, failover, and self-healing are on the roadmap.

---

## What it does

An authenticated user can upload, list, download, and delete files through a REST API. Each file is:

- Stored as bytes in **MinIO** (S3-compatible object storage)
- Recorded as metadata in **PostgreSQL** (filename, size, type, owner, SHA-256 checksum, storage key)
- Verified with a **SHA-256 checksum** for integrity
- **Owned** by the uploading user — you can only access your own files

---

## Tech stack

- **Language:** Java 21
- **Framework:** Spring Boot 3.3.5 (Spring MVC, Spring Data JPA, Spring Security)
- **Authentication:** JWT (jjwt), BCrypt password hashing
- **Database:** PostgreSQL 16, schema managed with Flyway migrations
- **Object storage:** MinIO (S3-compatible)
- **API docs:** OpenAPI / Swagger UI
- **Testing:** JUnit 5, Mockito, Testcontainers
- **Build & infra:** Maven, Docker, Docker Compose

---

## Features by phase

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
- Correct status codes: 201 register, 401 unauthenticated, 409 duplicate email

**Phase 3 — File storage**
- Upload files (bytes to MinIO, metadata to PostgreSQL)
- SHA-256 checksum computed on upload
- List, download, and delete your files
- Per-user ownership enforced on every operation
- Storage abstracted behind an interface (ready for multi-node backends)

---

## API endpoints

| Method | Path                        | Auth  | Description                        |
|--------|-----------------------------|-------|------------------------------------|
| GET    | /api/health                 | No    | App + database health              |
| POST   | /api/auth/register          | No    | Create an account, returns a JWT   |
| POST   | /api/auth/login             | No    | Verify credentials, returns a JWT  |
| POST   | /api/files                  | Yes   | Upload a file (multipart)          |
| GET    | /api/files                  | Yes   | List your files                    |
| GET    | /api/files/{id}/download    | Yes   | Download one of your files         |
| DELETE | /api/files/{id}             | Yes   | Delete one of your files           |

Interactive docs: `http://localhost:8080/swagger-ui.html`

---

## Getting started — run it on your machine

Anyone can clone this repository and run the full application locally in a few minutes.

### Prerequisites

- **JDK 21** — check with `java -version` (must show 21.x)
- **Maven 3.9+** — check with `mvn -version` (macOS: `brew install maven`)
- **Docker Desktop** — must be installed and running

### 1. Clone the repository

```bash
git clone https://github.com/Nareshtiwari74/distributed-file-storage.git
cd distributed-file-storage
```

### 2. Start PostgreSQL and MinIO

```bash
docker compose up -d postgres minio
```

Defaults (no setup needed):
- PostgreSQL: database `dfs`, user `dfs`, password `dfs`
- MinIO: user `minioadmin`, password `minioadmin`

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

---

## Full walkthrough — register, upload, download, delete

```bash
# 1. Register (returns a JWT). Save the token.
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"you@example.com","password":"password123"}'

# 2. Store the token in a variable (copy it from the response above)
TOKEN="paste-your-token-here"

# 3. Create a test file and upload it
echo "hello world" > myfile.txt
curl -X POST http://localhost:8080/api/files \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@myfile.txt"

# 4. List your files
curl http://localhost:8080/api/files \
  -H "Authorization: Bearer $TOKEN"

# 5. Download file with id 1
curl http://localhost:8080/api/files/1/download \
  -H "Authorization: Bearer $TOKEN" -o downloaded.txt

# 6. Delete file with id 1
curl -X DELETE http://localhost:8080/api/files/1 \
  -H "Authorization: Bearer $TOKEN"
```

You can also watch uploaded files appear in the MinIO console at
`http://localhost:9001` (login `minioadmin` / `minioadmin`).

---

## Running the whole stack in Docker (optional)

Instead of steps 2–3, build and run everything (app + database + storage) in containers:

```bash
docker compose up --build
```

---

## Stopping

```bash
# Stop the app with Ctrl+C, then:
docker compose down       # stop containers
docker compose down -v    # also wipe database + storage volumes (fresh start)
```

---

## Testing

```bash
mvn test      # unit and web-layer tests
mvn verify    # also runs integration tests (Testcontainers; requires Docker)
```

Integration tests are tagged `integration` and run separately from the fast unit-test feedback loop, so they can be enabled in CI without slowing local builds.

---

## Architecture

Modular monolith — one deployable, split internally by feature package (`auth`, `health`, `file`, `config`, `common`, `user`). Distribution lives in the storage layer, not in splitting the application into microservices.

**How a file is stored:**