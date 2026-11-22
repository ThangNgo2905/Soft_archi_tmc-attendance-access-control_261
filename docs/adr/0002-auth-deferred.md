# ADR 0002: Authentication Deferred (RBAC Documented)

## Status

Accepted

## Context

Actors require distinct permissions: employee, production supervisor, HR, security guard, system administrator, executive. Full JWT or session-based RBAC is needed eventually but slows the structure-first scaffold.

## Decision

1. **Document** the target RBAC matrix now.  
2. **Do not implement** authentication/authorization in this increment.  
3. Configure Spring Security in **dev/scaffold mode** to permit `/api/**`, OpenAPI, and Actuator health endpoints.  
4. Choose JWT vs Redis-backed sessions in a later ADR when auth is implemented.

## Target RBAC matrix (to implement later)

| Capability | Employee | Supervisor | HR | Security | Admin | Executive |
|------------|:--------:|:----------:|:--:|:--------:|:-----:|:---------:|
| View own QR / attendance | Y | Y | Y | Y | Y | Y |
| Manage users / QR | | | Y | | Y | |
| Manage gates | | | | Y | Y | |
| Record / view gate events | | Y | Y | Y | Y | |
| Reports & export | | Y | Y | | Y | Y |
| System configuration | | | | | Y | |

## Consequences

**Positive** — faster API and UI scaffolding; OpenAPI usable without tokens.  
**Negative** — APIs are open in local/dev; must not be treated as production-ready.  

## Follow-up

- ADR for JWT or session strategy  
- Role claims mapped to Spring `@PreAuthorize`  
- Frontend route guards aligned with roles  
