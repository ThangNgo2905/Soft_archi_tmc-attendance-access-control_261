# Backend Coding Rules

Package root: `com.tmc.aac`.

## Layer dependency rules

Allowed dependencies:

```text
controller      -> service, shared
service         -> domain, repository, shared
domain          -> shared only (no Spring / JPA annotations preferred on pure domain types when practical)
repository      -> domain, shared (interfaces + adapters: JPA, in-memory, HTTP clients)
configuration   -> (Spring only; no feature services)
```

Forbidden:

- Controllers calling repositories or HTTP clients directly  
- Domain depending on Spring Web or JPA details when avoidable  
- Circular feature dependencies (`user` ↔ `attendance` without shared events/ports)
- Feature code depending on `configuration` types

## Package layout

```text
com.tmc.aac.features.<feature>/   # attendance, gate, user, reporting, integration
  controller/       # *Controller, feature request/response DTOs
  service/          # *Service (use cases)
  domain/           # entities, value objects, domain services
  repository/       # persistence/query ports and implementations

com.tmc.aac.configuration/        # all Spring @Configuration beans

com.tmc.aac.shared/
  api/              # ApiResponse (+ shared request/response wrappers)
  exception/        # GlobalExceptionHandler + shared exceptions
  controller/       # cross-cutting system endpoints (e.g. meta)
```


## SOLID expectations

- **S** — one application service use-case cluster per concern  
- **O** — extend via new adapters/ports, not by editing core services  
- **L** — port implementations must honor interface contracts  
- **I** — small ports (e.g. `TurnstilePort`, not a mega `ExternalSystems`)  
- **D** — services depend on repository interfaces; repository package provides beans  

## Sharing context with coding agents (any tool)

**Single entry point:** [AGENTS.md](../../AGENTS.md) at the repo root.

All agents (any IDE/tool) start there. This file holds backend layers, SOLID, and **Agent pre-flight** only — do not duplicate those into IDE configs. Tool stubs (`CLAUDE.md`, `.github/copilot-instructions.md`, local `.cursor/`) must only redirect to `AGENTS.md`.

## Agent pre-flight — answer before writing code

Before adding or changing backend code, the agent must **read this file** and briefly answer (in the chat or plan) the questions below. If any answer violates a rule, redesign before coding.

### Layers and packages

1. Which **feature package** under `com.tmc.aac.features` (`user`, `gate`, `attendance`, …) owns this change?
2. Which **layer** will each new type live in (`controller`, `service`, `domain`, `repository`)?
3. Do all new dependencies follow the allowed arrows (controller → service → domain; service → repository only via interfaces)?
4. Will any controller import a class from `repository` or talk to HTTP/DB directly? (Must be **no**.)

### SOLID — single responsibility (S)

5. What is the **one reason to change** for each new class you are adding?
6. If a service method does validation, persistence, and external calls in one block, how will you split responsibilities across layers?

### SOLID — open/closed (O)

7. Can the new behavior be added by a **new repository adapter** or interface method instead of editing unrelated services?
8. What would a second implementation of the same port look like (e.g. JPA instead of in-memory)?

### SOLID — Liskov substitution (L)

9. For each new `repository` implementation, does it honor the interface contract (no surprise exceptions, no weakening preconditions)?
10. If you return domain types from a repository, can callers rely on the same invariants as with existing implementations?

### SOLID — interface segregation (I)

11. Is the new repository interface limited to what **one service** needs, or are you creating a “god port”?
12. Can you split a wide interface into two smaller ports without breaking callers?

### SOLID — dependency inversion (D)

13. Does the service depend on a **repository interface**, with construction via Spring injection (not `new` on concrete adapters)?
14. Are domain types free of Spring/JPA annotations where practical?

### Done definition

15. Will `mvn -B test` pass after the change?
16. Did you avoid new cross-feature imports unless documented in `shared` or an ADR?

## API conventions

- Base path: `/api/v1/...`  
- Prefer `ApiResponse<T>` wrapper from `shared`  
- Validation via Jakarta Validation on request DTOs  
- Document with springdoc OpenAPI  

## Persistence

- Flyway migrations under `src/main/resources/db/migration`  
- JPA entities map tables; schema owned by Flyway (not `ddl-auto=update` in shared environments)  

## Security (current)

- Permit-all for API/docs/health in scaffold (see ADR 0002)  
- Do not log secrets; future auth must not break layer rules  

## Testing

- Unit-test services with mocked repository interfaces  
- Slice/integration tests for controllers and JPA as features grow  
- `mvn -B test` must pass in CI  
