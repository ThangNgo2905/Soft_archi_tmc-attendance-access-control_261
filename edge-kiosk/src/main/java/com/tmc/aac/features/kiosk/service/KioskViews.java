package com.tmc.aac.features.kiosk.service;

import com.tmc.aac.features.kiosk.domain.AccessTransaction;
import java.time.Instant;
import java.util.List;

public final class KioskViews {
    private KioskViews() {}
    public record TransactionView(String transactionId, String scanId, String employeeId, String gateId,
            String direction, String state, String verificationMode, String reason, int sequence,
            Instant occurredAt, Instant updatedAt) {
        public static TransactionView from(AccessTransaction tx) {
            return new TransactionView(tx.transactionId(), tx.scanId(), tx.employeeId(), tx.gateId(),
                    tx.direction().name(), tx.state().name(), tx.verificationMode().name(), tx.reason(),
                    tx.sequence(), tx.occurredAt(), tx.updatedAt());
        }
    }
    public record StatusView(String kioskId, String factoryId, String gateId, String direction,
            int pendingEvents, boolean coreConnected, long cacheVersion, List<TransactionView> transactions) {}
    public record SampleView(String kind, String payload) {}
}
