package com.tmc.aac.features.kiosk.repository;
import com.tmc.aac.features.kiosk.domain.IntegrationContracts.AttendanceEvent;
import java.time.Instant;
import java.util.List;
public interface OutboxStore {
    record PendingEvent(AttendanceEvent event, int attempts) {}
    List<PendingEvent> due(Instant now);
    void acknowledge(String eventId);
    void retry(String eventId, Instant nextAttempt);
    int pendingCount();
}
