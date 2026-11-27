# TMC Attendance & Access Control (AAC)

QR code-based Attendance and Access Control System for TMC Manufacturing (course project: software architecture).

## Tech stack

- **Backend:** Java 17, Spring Boot, PostgreSQL, Flyway, classic layered architecture (`com.tmc.aac`)
- **Frontend:** TypeScript, React, Vite, MUI
- **Ops:** Docker Compose
- **Mocks:** HR/Payroll + Turnstile APIs (`mock-servers/`)

## Documentation

| Doc | Description |
|-----|-------------|
| [docs/architecture/README.md](docs/architecture/README.md) | Architecture overview (context, containers, layers) |
| [docs/architecture/functional-context.md](docs/architecture/functional-context.md) | Actors, FR/NFR, factories |
| [docs/architecture/integration.md](docs/architecture/integration.md) | External APIs |
| [docs/adr/](docs/adr/) | Architecture Decision Records |
| [docs/rules/backend.md](docs/rules/backend.md) | Backend coding rules |
| [docs/rules/frontend.md](docs/rules/frontend.md) | Frontend coding rules |

## Quick start

### Prerequisites

- Docker Desktop (Compose v2)
- Optional for local non-Docker dev: JDK 17, Maven, Node 20+

### Run from current workspace (build local images)

**Windows (PowerShell):**

```powershell
.\scripts\up-local.ps1
```

**Linux / macOS:**

```bash
./scripts/up-local.sh
```

This builds and starts Postgres, Redis, mock servers, backend, and frontend from your checkout.

### Useful URLs (default)

| Service | URL |
|---------|-----|
| Frontend (Compose nginx or `npm run dev`) | http://localhost:5173 |
| Backend API | http://localhost:8080 |
| OpenAPI / Swagger | http://localhost:8080/swagger-ui.html |
| Mock HR / Turnstile | http://localhost:4000 |
| Postgres | localhost:5432 |

## Local development (without full Compose apps)

```bash
# Infrastructure only
docker compose up -d postgres redis mock-servers

# Backend (http://localhost:8080)
cd backend && ./mvnw spring-boot:run

# Frontend (http://localhost:5173 — Vite proxies /api → :8080)
cd frontend && npm install && npm run dev
```

## Troubleshooting

| Issue | Fix |
|-------|-----|
| Backend health / API not up | Ensure Postgres is healthy (`docker compose ps`); wait for backend start (~60s); ports **8080** free |
| Frontend 502 after Compose up | Frontend waits until backend is healthy; check `docker compose logs backend` |
| Local Vite API calls fail | Leave `VITE_API_BASE_URL` empty and use the Vite proxy to **8080**, or set it to `http://localhost:8080` |

## Scope note

The independent **Edge Kiosk & Scanner demo** now lives in [`edge-kiosk/`](edge-kiosk/README.md).
Run `docker compose -f docker-compose.edge.yml up --build -d` for the kiosk and
simulated Core/Registry/Turnstile, without starting the central stack. The existing
backend remains a scaffold; the demo does not implement Core ingestion.

This repository currently scaffolds **structure and docs**. Core domain features (QR, gate unlock, RBAC) are implemented in later increments. See [ADR 0002](docs/adr/0002-auth-deferred.md) for deferred auth.
