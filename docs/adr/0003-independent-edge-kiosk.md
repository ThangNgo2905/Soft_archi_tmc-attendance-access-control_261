# ADR 0003: Independent Java Edge Kiosk for the course demo

## Status

Accepted by the feature owner during planning, November 2026.

## Context

The group design requires QR decisions at each lane even without a WAN connection.
The PDF lists Kotlin/Android or .NET/mini-PC for Edge. The team selected Java/Spring
Boot for a computer demo so it can use the same language and layer conventions as
Core. This does not change Core's layered monolith decision in ADR 0001.

## Decision

- Add an independently deployable `edge-kiosk/` application, with one process and
  one SQLite database per lane. Never share that file between kiosk instances.
- Keep `com.tmc.aac.features.kiosk` layers and dependency inversion. No Java
  dependency on Core feature classes. DTOs and wire contracts cross HTTP boundaries.
- Use SQLite JDBC and versioned SQL migrations in an Edge-owned migration runner.
  JDBC keeps the domain free of persistence annotations; central PostgreSQL/Flyway
  remains unchanged. SQL schema changes are transactional and version checked.
- Commit `PENDING_AUTHORIZATION` before Registry access, and
  `GATE_COMMAND_PENDING` before a gate command. Commit outcome, transition history,
  local state and outbox together. Recover by querying the same transaction ID.
- A connection failure certified to occur before authorization permits local
  fallback. An ambiguous authorization timeout stays pending; it must not bypass
  an existing reservation. Turnstile uncertainty becomes `OPEN_UNKNOWN`; recovery
  never blindly reissues an open command.
- Event IDs deduplicate transport retries; `(transactionId, sequence)` distinguishes
  later reconciliation. This versioned event contract requires agreement with the
  real Core team before integration; it is not an existing Core API.
- Serve a React/TypeScript/MUI demo UI locally. Input is pasted payloads and server
  generated sample tokens. All domain decisions remain in Java.
- Keep simulated Core, Registry and Turnstile in a separate Python standard-library
  process, on independent ports. Persistent simulator state supports restart demos,
  not Registry HA or production security guarantees.
- Provide `docker-compose.edge.yml` without altering the existing Core stack.

## Consequences

The branch can merge into the same repository while Edge builds and runs separately.
Fixtures and mock controls are available only in the `demo` Spring profile. Default
host bindings are loopback; the demo uses synthetic identities and a public demo key.
It does not implement device provisioning, mTLS, SQLCipher/TPM, real scanner SDKs,
Registry replication or the real Core ingestion API. Those remain requirements for
production integration. A mock cannot verify the physical two-second opening target.
