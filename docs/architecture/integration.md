# External Integrations

The AAC system depends on two external systems already operated by TMC. In development and CI, they are replaced by [`mock-servers/`](../../mock-servers/).

## Turnstile Controller API

**Purpose:** Unlock a physical gate after a successful QR access check.

| Item | Value |
|------|--------|
| Mock base URL | `http://mock-servers:4000` (Docker) / `http://localhost:4000` (host) |
| Endpoint | `POST /api/turnstile/unlock` |
| Request body | `{ "gateId": string, "transactionId": string }` |
| Success | `{ "success": true, "status": "OPEN_CONFIRMED" }` |

Adapter location (scaffold): `com.tmc.aac.features.integration` — ports and HTTP clients under `repository/`.

## HR / Payroll Sync API

**Purpose:** Transmit attendance batches for reconciliation (must be ready before 12:00 PM on the 1st of each month).

| Item | Value |
|------|--------|
| Mock base URL | same as above |
| Endpoint | `POST /api/hr/sync` |
| Request body | `{ "records": [ ... ] }` |
| Success | `{ "success": true, "count": number }` |

## Configuration

Backend properties (examples):

```properties
tmc.integration.turnstile.base-url=http://localhost:4000
tmc.integration.hr.base-url=http://localhost:4000
```

In Docker Compose, override to `http://mock-servers:4000`.

## Future work

- Idempotent unlock and sync with retry/backoff  
- Scheduled monthly export job  
- Replace mock URLs with production endpoints and authentication  
