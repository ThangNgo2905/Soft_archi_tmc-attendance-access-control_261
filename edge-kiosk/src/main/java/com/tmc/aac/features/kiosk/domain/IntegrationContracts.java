package com.tmc.aac.features.kiosk.domain;

import java.time.Instant;
import java.util.List;

/** Typed wire contracts shared by the Edge ports, without framework dependencies. */
public final class IntegrationContracts {
    private IntegrationContracts() {}
    public record Authorization(String transactionId, String factoryId, String employeeId, String nonce, Direction direction) {}
    public enum Decision { ALLOWED, DENIED, NOT_FOUND }
    public record AuthorizationResult(Decision decision, String reason) {}
    public record GateCommand(String transactionId, String gateId) {}
    public enum GateOutcome { OPEN_CONFIRMED, DENIED, OPEN_UNKNOWN, NOT_FOUND }
    public record GateResult(GateOutcome outcome) {}
    public record RegistryOutcome(String transactionId, AccessState state) {}
    public record AttendanceEvent(String eventId, String transactionId, int sequence, String kioskId,
            String factoryId, String gateId, String employeeId, Direction direction, AccessState state,
            VerificationMode verificationMode, String reason, Instant occurredAt, Instant updatedAt) {}
    public record Batch(List<AttendanceEvent> events) {}
    public record BatchAck(List<String> acknowledgedEventIds) {}
    public record CacheSnapshot(long version, List<EmployeeAccess> employees) {}
    public record Heartbeat(String kioskId, String factoryId, String gateId, Direction direction, int pendingEvents, Instant at) {}
}
