# ADR 0001: Classic Layered Monolith

## Status

Accepted

## Context

The Spring Initializr scaffold included Spring Modulith. The project requires a clear layered architecture that students can map to SOLID and multi-view documentation. Modulith adds module boundaries that are useful later but increase early scaffolding complexity.

## Decision

Use a **single deployable Spring Boot monolith** with **classic layers** under package root `com.tmc.aac`:

- Feature packages under `com.tmc.aac.features`: `user`, `gate`, `attendance`, `reporting`, `integration`  
- Within each feature (where applicable): `controller` → `service` → `domain` ← `repository`  
- Cross-cutting: `configuration` (Spring `@Configuration`), `shared` (`api`, `exception`, system controllers)  
- Services depend on **repository interfaces**; JPA/in-memory/HTTP adapters implement them  

Remove Spring Modulith dependencies from `pom.xml`.

## Consequences

**Positive**

- Easy to teach and review layer rules  
- Clear DIP points for tests and mocks  
- Matches course expectation of “layered architecture”  

**Negative**

- No compile-time module boundaries (discipline via docs/rules)  
- May revisit Modulith or modular monolith if the codebase grows  

## Alternatives considered

1. Keep Modulith with nested layers — deferred  
2. Microservices per feature — out of scope for course milestones  
