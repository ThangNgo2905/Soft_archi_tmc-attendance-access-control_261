package com.tmc.aac.features.kiosk.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RecoveryHandler {
    private static final Logger LOG = LoggerFactory.getLogger(RecoveryHandler.class);
    private final AccessTransactionCoordinator coordinator;
    private final SyncAgent sync;
    private final boolean enabled;
    public RecoveryHandler(AccessTransactionCoordinator coordinator, SyncAgent sync,
            @Value("${edge.workers-enabled}") boolean enabled) {
        this.coordinator = coordinator; this.sync = sync; this.enabled = enabled;
    }
    @Scheduled(fixedDelayString = "${edge.worker-delay}", initialDelayString = "${edge.worker-delay}")
    public void tick() {
        if (!enabled) return;
        try { coordinator.recover(); }
        catch (RuntimeException ex) { LOG.error("Recovery failed; durable work retained ({})", ex.getClass().getSimpleName()); }
        try { sync.synchronize(); }
        catch (RuntimeException ex) { LOG.error("Synchronization failed; outbox retained ({})", ex.getClass().getSimpleName()); }
    }
}
