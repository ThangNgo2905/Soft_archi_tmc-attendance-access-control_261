package com.tmc.aac.features.kiosk.domain;

import java.time.Instant;

public record AccessTransaction(String transactionId, String scanId, String requestDigest,
        String employeeId, String nonce, String kioskId, String factoryId, String gateId, Direction direction,
        AccessState state, VerificationMode verificationMode, String reason, Instant occurredAt,
        Instant updatedAt, int sequence, boolean registryPending) {

    public AccessTransaction transition(AccessState target, VerificationMode mode, String explanation, Instant now, boolean notifyRegistry) {
        boolean allowed = switch (state) {
            case PENDING_AUTHORIZATION -> target == AccessState.GATE_COMMAND_PENDING || target == AccessState.DENIED;
            case GATE_COMMAND_PENDING -> target == AccessState.OPEN_CONFIRMED || target == AccessState.DENIED || target == AccessState.OPEN_UNKNOWN;
            case OPEN_UNKNOWN -> target == AccessState.OPEN_CONFIRMED || target == AccessState.DENIED;
            default -> false;
        };
        if (!allowed) throw new IllegalStateException("Invalid access state transition");
        return new AccessTransaction(transactionId, scanId, requestDigest, employeeId, nonce, kioskId, factoryId,
                gateId, direction, target, mode, explanation, occurredAt, now, sequence + 1, notifyRegistry);
    }
}
