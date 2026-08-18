# Distributed File Storage System

A full-stack, fault-tolerant file storage application built with **Spring Boot** and **Java 21**, with a **React** frontend — a small, understandable take on what systems like Amazon S3 or Google Drive do under the hood: store files reliably so that no single machine failure loses data or makes it unavailable.

File **contents** are stored in S3-compatible object storage (MinIO locally, Supabase in production); file **metadata** lives in PostgreSQL. The backend is a **modular monolith**, built phase by phase with automated tests and documentation at each step.

> **Status:** Phases 1–3 complete, with a live frontend, and **fully deployed** (foundation, authentication, file storage). Multi-node replication, failover, and self-healing are on the roadmap.

---

## 🔗 Live Demo

**Web app (frontend):** https://distributed-file-storage-two.vercel.app
**API:** https://distributed-file-storage-b26g.onrender.com

- Health: [/api/health](https://distributed-file-storage-b26g.onrender.com/api/health)
- API docs (Swagger): [/swagger-ui.html](https://distributed-file-storage-b26g.onrender.com/swagger-ui.html)

Fully deployed and functional — register/login (JWT) and file upload, list, download, and delete, all live through the web app or the API directly. Files are stored in cloud object storage.

**Deployed stack:** React on Vercel · Spring Boot on Render · PostgreSQL on Neon · S3-compatible object storage (Supabase)

> Runs on free tiers. The first request after inactivity may take ~30–50s while the backend wakes.

---

## What it does

A registered user can upload, list, download, and delete files — through a simple web interface or the REST API. Each file is:

- Stored as bytes in **S3-compatible object storage**
- Recorded as metadata in **PostgreSQL** (filename, size, type, owner, SHA-256 checksum, storage key)
- Verified with a **SHA-256 checksum** for integrity
- **Owned** by the uploading user — you can only access your own files

**Limits:** up to **10 MB per file**, any file type. Total storage is bounded by the free-tier quota (~1 GB). Oversized uploads are rejected with a clear `413` response.

---

## Tech stack

**Backend**
- **Language:** Java 21
- **Framework:** Spring Boot 3.3.5 (Spring MVC, Spring Data JPA, Spring Security)
- **Authentication:** JWT (jjwt), BCrypt password hashing
- **Database:** PostgreSQL, schema managed with Flyway migrations
- **Object storage:** S3-compatible (MinIO locally, Supabase in production) via the AWS SDK
- **API docs:** OpenAPI / Swagger UI
- **Testing:** JUnit 5, Mockito, Testcontainers
- **Build & infra:** Maven, Docker, Docker Compose

**Frontend**
- **React** (single-page app) with fetch-based API integration and JWT auth
- Client-side email/password validation, show-password toggle

**Deployment**
- Frontend on **Vercel** · Backend on **Render** · Database on **Neon** · Object storage on **Supabase**

---

## Repository structure
distributed-file-storage/
├── src/ # Spring Boot backend (Java)
├── frontend/ # React web app
├── pom.xml # backend build
├── docker-compose.yml # local Postgres + MinIO
└── README.md

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
- Upload files (bytes to object storage, metadata to PostgreSQL)
- SHA-256 checksum computed on upload
- List, download, and delete your files
- Per-user ownership enforced on every operation
- 10 MB upload limit with a clean error for oversized files
- Storage abstracted behind an interface (ready for multi-node backends)

**Frontend + Deployment**
- React web app: login/register, upload, file list, download, delete
- Full stack deployed live (Vercel + Render + Neon + Supabase)

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

Interactive docs: `/swagger-ui.html`

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

Defaults (local development only — never used in production):
- PostgreSQL: database `dfs`, user `dfs`, password `dfs`
- MinIO: user `minioadmin`, password `minioadmin`

### 3. Run the backend

```bash
mvn spring-boot:run
```

Wait for `Started DistributedFileStorageApplication`. The API is live at `http://localhost:8080`.

### 4. Verify it works

```bash
curl http://localhost:8080/api/health
```

Expected: `{"status":"UP","components":{"database":"UP"}}`

### 5. Open the frontend

Open `frontend/index.html` in your browser. (It points at the deployed API by default; edit the `API` constant at the top of the file to use `http://localhost:8080` for local development.)

---

## Full walkthrough — register, upload, download, delete (via API)

```bash
# 1. Register (returns a JWT)
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

The backend is a **modular monolith** — one deployable, split internally by feature package (`auth`, `health`, `file`, `config`, `common`, `user`). Distribution lives in the storage layer, not in splitting the application into microservices.

**Full-stack flow:**
React frontend (Vercel)
│ HTTPS + JWT
▼
Spring Boot API (Render)
│
├──► StorageService (S3-compatible) → file BYTES → Supabase
│
└──► FileMetadataRepository → METADATA → PostgreSQL (Neon)

**How a file is stored:**
Upload request (+ JWT)
│
▼
JwtAuthenticationFilter → identifies the user from the token
│
▼
FileController → FileService
│ │
│ ├──► StorageService (S3-compatible) → stores the file BYTES
│ │
│ └──► FileMetadataRepository → stores METADATA in PostgreSQL
▼
Returns file metadata (id, name, size, checksum)

- **Metadata** (small, queryable) → PostgreSQL
- **Bytes** (large, binary) → object storage
- Linked by an `object_key` — the metadata row points to the object in storage

The `StorageService` interface keeps the storage backend swappable — the same code runs against local MinIO in development and Supabase in production, and is ready for multi-node backends later.

Database schema is versioned with Flyway migrations under `src/main/resources/db/migration`.

---

## Configuration

All environment-specific settings are supplied via environment variables (with safe local defaults). Secrets are never committed.

| Variable | Purpose |
|----------|---------|
| `SPRING_DATASOURCE_URL` / `USERNAME` / `PASSWORD` | PostgreSQL connection |
| `JWT_SECRET` | Signing key for JWTs (long random value in production) |
| `STORAGE_ENABLED` | Toggles the object-storage backend |
| `MINIO_ENDPOINT` / `MINIO_USER` / `MINIO_PASSWORD` / `MINIO_BUCKET` / `MINIO_REGION` | S3-compatible storage connection |
| `CORS_ALLOWED_ORIGINS` | Allowed frontend origins (comma-separated) |
| `PORT` | Server port (set automatically by the host) |

---

## Roadmap

- [x] **Phase 1** — Foundation (health, config, error handling, logging, Docker)
- [x] **Phase 2** — Authentication (register, login, JWT, route protection)
- [x] **Phase 3** — File upload / list / download / delete with S3-compatible storage + SHA-256
- [x] **Frontend** — React web app (login, upload, list, download, delete)
- [x] **Deployment** — Live full stack (Vercel + Render + Neon + Supabase)


---
