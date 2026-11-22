# Architecture Overview

QR code-based **Attendance and Access Control (AAC)** system for TMC Manufacturing Joint Stock Company (factories: Binh Duong ~800 employees, Long An ~600 employees).

## Tech stack

| Layer | Technology |
|-------|------------|
| Backend | Java 17, Spring Boot, JPA, PostgreSQL, Flyway |
| Frontend | TypeScript, React, Vite, MUI |
| Infra | Docker Compose, Redis (reserved) |
| External | HR/Payroll API, Turnstile Controller API (mocked in `mock-servers/`) |

## System context (C4 Level 1)

```text
                    +------------------+
                    |   Employees /    |
                    | Supervisors / HR |
                    | Security / Admin |
                    |   Executives     |
                    +--------+---------+
                             |
                             v
                    +------------------+
         +--------->|  TMC AAC System  |<---------+
         |          +--------+---------+          |
         |                   |                    |
         v                   v                    v
+----------------+  +----------------+  +------------------+
| Smartphone /   |  | PostgreSQL     |  | HR/Payroll API   |
| Web portal QR  |  | (attendance)   |  | Turnstile API    |
+----------------+  +----------------+  +------------------+
```

Actors interact via the web portal (and future mobile). Gate scanners trigger attendance events; the system unlocks turnstiles and later syncs attendance to HR before 12:00 PM on the 1st of each month.

## Containers (C4 Level 2)

| Container | Role |
|-----------|------|
| `frontend` | SPA (React + MUI): admin/HR/reporting UI, employee QR display stub |
| `backend` | Spring Boot REST API: layered monolith (`com.tmc.aac`) |
| `postgres` | Primary persistence |
| `redis` | Reserved (sessions/cache in later increments) |
| `mock-servers` | Stand-in for HR sync and turnstile unlock APIs |

## Backend components (C4 Level 3)

Classic layered packages under `com.tmc.aac.features` (`user`, `gate`, `attendance`, `reporting`, `integration`):

- **Controller** — REST controllers, request/response DTOs  
- **Service** — use-case services  
- **Domain** — entities, domain services  
- **Repository** — persistence/query ports and adapters (JPA, in-memory, HTTP clients)  

Cross-cutting:

- **`configuration`** — Spring `@Configuration` (security, OpenAPI, RestClient, …)  
- **`shared`** — `api` (e.g. `ApiResponse`), `exception` (`GlobalExceptionHandler`), system controllers  

Dependency rule: `controller` → `service` → `domain` ← `repository` (DIP via repository interfaces).

## Frontend structure

- `app/` — providers, theme, router  
- `pages/` — route-level screens  
- `components/` — layout and shared UI  
- `features/` — domain hooks/types (filled in later)  
- `api/` — HTTP client toward backend OpenAPI  

## Deployment view

Operator mode: build and run from the current checkout via `scripts/up-local` / `docker compose up --build`.

## Related docs

- [Functional context](./functional-context.md)  
- [Integration](./integration.md)  
- [Backend rules](../rules/backend.md)  
- [Frontend rules](../rules/frontend.md)  
- ADRs under [`docs/adr/`](../adr/)  
