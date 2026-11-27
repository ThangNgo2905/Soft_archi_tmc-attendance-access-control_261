# Edge Kiosk & Scanner demo

Independent Spring Boot application for one gate lane. React/MUI UI runs locally;
SQLite stores real transaction history and outbox records. Core, Factory Replay
Registry and Turnstile are contract simulators. Core's existing source and Compose
stack are unchanged. See [ADR 0003](../docs/adr/0003-independent-edge-kiosk.md).

## Run with Docker Desktop

From the repository root:

```powershell
docker compose -f docker-compose.edge.yml up --build -d
```

Open http://localhost:8090. Image build runs Java tests and UI lint/build. Named
volumes retain Edge and mock databases across container recreation.

For both an IN and an OUT lane sharing a Registry:

```powershell
docker compose -f docker-compose.edge.yml --profile two-lanes up --build -d
```

IN is at http://localhost:8090, OUT at http://localhost:8091. Each has its own database.
Create a fresh sample for each scan. After a successful IN, a second IN for the same
employee is denied until an OUT is confirmed. Return mock modes to NORMAL before
testing the next case. History and anti-passback state deliberately persist.

Stop with `docker compose -f docker-compose.edge.yml --profile two-lanes down`.
Do not add `--volumes` if you want to keep demo data.

## Local development

Prerequisites: JDK 17+ (set JAVA_HOME), Node 20.19+/22+, Python 3.10+.
Maven Wrapper downloads Maven on its first run. Run commands in separate terminals.

```powershell
# Terminal 1, repository root
python edge-kiosk/demo-mocks/server.py --database edge-kiosk/data/mock.db

# Terminal 2
cd edge-kiosk
.\mvnw.cmd -B spring-boot:run

# Terminal 3
cd edge-kiosk/ui
npm ci
npm run dev
```

Open http://localhost:5174 during development. Vite proxies `/api` to the local
Edge at port 8090, never to the central backend. To serve the built UI from Edge:
run `npm run build` in `ui/`, then `./mvnw -B package` in `edge-kiosk/` and
`java -jar target/edge-kiosk-0.0.1-SNAPSHOT.jar`. Do not run Maven `clean` between
the UI build and packaging; that would remove the generated static assets.

## Demo cases

1. **Happy path:** create a valid sample, scan, observe OPEN_CONFIRMED and one
   physical open in the mock counters. Wait for pending events to reach zero.
2. **Validation:** try expired, bad-signature, wrong-factory, outside-shift and
   inactive samples. Each is denied without a gate command. Samples are generated
   fresh; valid tokens expire after 30 seconds.
3. **Core outage:** set Core DOWN, scan a valid transaction, observe the outbox.
   Set NORMAL and wait for retry (backoff up to 60 seconds). The same event IDs
   reach Core exactly once in its event store.
4. **Lost Core ACK:** LOST_ACK persists the batch but delays its response beyond
   Edge's timeout. Set NORMAL. Retries retain IDs and do not duplicate events.
5. **Registry outage:** DOWN explicitly certifies requests were not processed.
   Edge uses local nonce/anti-passback and marks OFFLINE_UNVERIFIED. That flag is
   retained in Core; the demo does not claim cross-kiosk verification for fallback.
   Use the second employee sample for a fresh local history. An OUT at another
   kiosk is not visible in this kiosk's local cache; stale local IN can correctly
   cause LOCAL_ANTI_PASSBACK during fallback and requires reconciliation.
6. **Registry lost ACK:** LOST_ACK commits authorization but delays its response.
   The scan stays pending, then recovery queries the same ID. It never changes
   into an unverified fallback transaction.
7. **Turnstile uncertainty:** UNKNOWN returns an unresolved result. Observe
   OPEN_UNKNOWN and the reservation. Repeated recovery only queries status.
   Use the transaction's demo sensor buttons to resolve it; a new versioned event
   records the resolution with an audit entry, without a new open command.
8. **Turnstile lost ACK:** LOST_ACK commits an opening then delays its response.
   Status lookup establishes OPEN_CONFIRMED with only one opening.
9. **Restart:** keep the named volume and restart `edge-kiosk`. Committed pending
   transactions and outbox survive. Recovery queries controller status for any
   GATE_COMMAND_PENDING; NOT_FOUND is treated as unknown, never permission to reopen.

The same employee may already be IN after earlier cases. Use the OUT lane between
successful IN cases. A DOWN Turnstile with no recorded command produces an unknown
result; its NOT_FOUND does not prove a physical non-opening. Offline-unverified
transactions require future real Registry reconciliation; they are not silently
promoted when connectivity returns.

## Configuration

| Environment | Default |
|---|---|
| EDGE_PORT / EDGE_BIND_ADDRESS | 8090 / 127.0.0.1 |
| EDGE_DATABASE | data/kiosk.db, relative to working directory |
| KIOSK_ID / FACTORY_ID / GATE_ID | BD-IN-01 / BD / BD-GATE-01 |
| GATE_DIRECTION / EDGE_ZONE | IN / Asia/Ho_Chi_Minh |
| REGISTRY_URL / TURNSTILE_URL / CORE_URL | http://127.0.0.1:4101 / :4102 / :4103 |
| EDGE_WORKER_DELAY / EDGE_WORKERS_ENABLED | 2000 milliseconds / true |
| SPRING_PROFILES_ACTIVE | default profile is demo |

One Edge process owns one SQLite file and serializes its lane. Do not horizontally
scale the same lane/database. SQLite uses WAL, synchronous FULL, foreign keys and
a busy timeout. Do not place its data volume on an unreliable network filesystem.

## Tests

```powershell
cd edge-kiosk
.\mvnw.cmd -B test
cd ui
npm ci
npm run lint
npm run build
cd ../..
python -m unittest discover -s edge-kiosk/demo-mocks -v
```

Java tests cover policy boundaries, real SQLite atomicity/rollback, crash points,
ports and HTTP adapter contracts, retry/ACK handling and dependency rules. Python
tests cover shared Registry reservations, one-time nonce, idempotent commands,
Core deduplication/partial ACK, and mock restart persistence.

`python edge-kiosk/demo-mocks/smoke_demo.py` runs HTTP acceptance checks against
two already running lanes (8090/8091) and mocks (4101..4103). Use fresh dedicated
databases for that run: it creates transactions and assumes no prior IN/OUT state.
It checks WAN loss, lost ACKs, UNKNOWN recovery and local fallback. Normal Java
and mock unit tests create their own temporary databases.

Verified during implementation: 23 Java tests, 8 Python tests, the two-lane HTTP
acceptance script, restart with a pending outbox, and UI lint/build plus browser
checks. Compose configuration was validated; Docker Engine was unavailable on the
development host, so image build and container runtime remain to be exercised.

The loopback-only demo has synthetic fixture keys and unauthenticated mock controls.
Use no real employee data. SQLCipher, mTLS, real scanner/camera, operator RBAC and
hardware latency measurements belong to subsequent integration work.
