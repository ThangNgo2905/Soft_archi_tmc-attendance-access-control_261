package com.tmc.aac.features.kiosk.domain;

public enum AccessState {
    PENDING_AUTHORIZATION, GATE_COMMAND_PENDING, OPEN_CONFIRMED, DENIED, OPEN_UNKNOWN;

    public boolean isOutcome() {
        return this == OPEN_CONFIRMED || this == DENIED || this == OPEN_UNKNOWN;
    }
}
