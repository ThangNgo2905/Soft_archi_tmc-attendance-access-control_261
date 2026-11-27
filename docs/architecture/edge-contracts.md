# Edge demo contracts v1

These APIs are implemented by the Edge simulators, not by the existing Core.
All JSON fields are camelCase. Timestamps are UTC ISO-8601; directions are IN/OUT.
See `edge-kiosk/src/main/java/com/tmc/aac/features/kiosk/domain/IntegrationContracts.java`
for the typed wire models and `edge-kiosk/demo-mocks/server.py` for the simulator.

## QR token

```text
v1|employeeId|issuedAtEpochSecond|expiresAtEpochSecond|nonce|keyId|signature
```

HMAC-SHA256 signs the exact UTF-8 bytes of the first six pipe-separated fields.
Signature is unpadded Base64URL. Identifier fields use `[A-Za-z0-9_-]{1,100}`.
`issuedAt <= now < expiresAt` and `0 < expiresAt-issuedAt <= 30`. The Edge verifies
signature before relying on the identity, factory assignment or local shift.
The demo uses keyId `demo-key-v1` and a public synthetic fixture secret; never use
these credentials in a real environment. HMAC authenticates, it does not encrypt.

Local daily shift windows include the start and exclude the end. A start after
the end represents an overnight window; equal start/end represents an empty
window. This demo does not model dated shift assignments or grace periods.

## Local scanner API

- `POST /api/v1/kiosk/scans`: `{ "scanId": "UUID", "payload": "v1|..." }`.
  Reuse scanId for transport retry; a different payload with that ID yields 409.
  Domain denials return a persisted DENIED transaction (HTTP 200); malformed
  HTTP/validation requests yield 400. Payload maximum is 2048 characters.
- `GET /api/v1/kiosk/status`: device identity, Core connectivity, cache version,
  pending event count and up to 100 recent transactions. It never exposes secrets.
- `/api/v1/kiosk/demo/*`: fixture generation and simulator controls, demo profile only.
- OpenAPI is available at `/v3/api-docs` and `/swagger-ui.html`.

The local API wraps results in `{success,message,data}`. Simulator APIs below
return the typed body directly. They are separate contracts.

## Registry (port 4101)

- `POST /api/v1/authorizations`: transactionId, factoryId, employeeId, nonce, direction.
- `GET /api/v1/authorizations/{transactionId}`: look up the same authorization.
- Both return `{ "decision": "ALLOWED|DENIED|NOT_FOUND", "reason": "..." }`.
  An authorize operation normally returns ALLOWED or DENIED; NOT_FOUND is for lookup.
- `POST /api/v1/outcomes`: `{transactionId,state}` returns JSON `true` after commit.

Nonce consumption, anti-passback and reservation happen together in one mock DB
transaction. A final OPEN_CONFIRMED sets IN/OUT and releases reservation; DENIED
releases it; OPEN_UNKNOWN retains it. Reusing an ID with different content yields
409, and never grants a second authorization.

An explicit `503 {"notProcessed":true}` is a demo-only assertion that the request
had no side effects. Only that assertion or a connection-establishment failure on
an initial attempt allows local fallback. An ambiguous timeout stays pending;
recovery queries/reauthorizes with the same ID and does not choose fallback.

## Turnstile (port 4102)

- `POST /api/v1/commands`: `{transactionId,gateId}`.
- `GET /api/v1/commands/{transactionId}`.
- Return `{ "outcome": "OPEN_CONFIRMED|DENIED|OPEN_UNKNOWN|NOT_FOUND" }`.

Same command ID returns the persisted outcome and never opens twice. A changed
gate under that ID is a conflict. A query NOT_FOUND after a crash is inconclusive.
Only new GATE_COMMAND_PENDING created in the live scan flow sends a command;
recovery always queries. The simulator's demo resolution represents a later sensor
observation, not an automatic retry or a production operator authorization API.

## Core (port 4103)

- `POST /api/v1/events/batch`: `{ "events": [AttendanceEvent, ...] }`, max 100.
- Return `{ "acknowledgedEventIds": ["..."] }` after durable storage. Edge only
  acknowledges IDs present in its submitted batch; unacknowledged events retry.
- `GET /api/v1/cache?factoryId=BD`: `{version,employees:[...]}`. Monotonic version;
  the Edge atomically replaces a newer snapshot and retains its cache offline.
- `POST /api/v1/heartbeat`: kioskId, factoryId, gateId, direction, pendingEvents, at.

AttendanceEvent contains eventId, transactionId, sequence, kioskId, factoryId,
gateId, employeeId, direction, state, verificationMode, reason, occurredAt, updatedAt.
Transport retry preserves all fields and eventId. A later resolution creates a new
eventId and sequence. Core mock stores out-of-order versions without double insertion;
it rejects conflicting event IDs or transaction/sequence pairs. It does not build
production headcount/read models. OFFLINE_UNVERIFIED is retained for reconciliation.

## Simulator control, each service port

- `GET /demo/state`: mode and counters (no secret data).
- `POST /demo/mode`: `{mode}`. Registry: NORMAL/DOWN/DENY/LOST_ACK;
  Turnstile also supports UNKNOWN; Core supports NORMAL/DOWN/LOST_ACK/PARTIAL_ACK.
- Turnstile `POST /demo/resolve`: `{transactionId,outcome}` for an UNKNOWN command;
  outcome must be OPEN_CONFIRMED or DENIED. Edge's demo proxy records the attempt
  in its audit log and triggers recovery.

LOST_ACK commits then waits beyond the Edge read timeout; DOWN responds before
processing any business operation. Modes themselves reset to NORMAL after mock
restart; persistent transaction/nonce/event state remains.
