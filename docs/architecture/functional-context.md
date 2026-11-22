# Functional Context

## Business context

TMC Manufacturing Joint Stock Company operates two factories:

- **Binh Duong** — ~800 employees  
- **Long An** — ~600 employees  

The company needs a system that:

1. Controls employee access to the workplace via QR codes and turnstiles  
2. Records entry/exit as attendance transactions  
3. Supports monthly attendance reports and export to the existing HR/Payroll system  

**Out of scope (this phase):** visitor management; payroll calculation (export only).

## Actors

| Actor | Typical needs |
|-------|----------------|
| Employee | Display personal QR; view own attendance |
| Production supervisor | Monitor team attendance / gate activity |
| HR staff | Reconcile attendance; export to payroll |
| Security guard | Monitor gates; handle denied access |
| System administrator | Users, gates, system configuration |
| Executive management | Reports and monitoring dashboards |

Every employee has a smartphone that can show a QR code (mobile app, web portal, or email).

## Functional requirements (summary)

1. **User and QR Code Management** — identities, roles, QR lifecycle  
2. **Gate Station Management** — factories, gates, scanner association  
3. **Attendance Event Recording** — scan → validate → unlock turnstile → persist event  
4. **Reporting and Auditing** — daily/monthly reports, audit trails  
5. **System Administration** — configuration, health, integration settings  

## Non-functional requirements (targets)

| Concern | Target direction |
|---------|------------------|
| Availability | Gate path resilient; soft-fail HR sync with retry |
| Performance | Low-latency unlock decision at the gate |
| Security | Future RBAC by actor; protect PII and attendance data |
| Auditability | Immutable attendance event history |
| Integrability | Clear APIs toward HR and turnstile controllers |
| Operability | Docker Compose + CI/CD; health endpoints |

## Critical integration deadline

Attendance data must be available for HR reconciliation **before 12:00 PM on the first day of each month**. Scheduling and batch export will live under the `integration` package (documented now; implemented later).

## Factories and gates

Logical model (to be persisted when features are implemented):

- `Factory` (Binh Duong, Long An)  
- `GateStation` belonging to a factory  
- `AttendanceEvent` tied to employee, gate, direction (IN/OUT), timestamp  

## Design preference for documentation

This repository scaffolds structure and architecture first. Domain rules above guide package boundaries and ADRs; business logic is implemented in later increments.  
