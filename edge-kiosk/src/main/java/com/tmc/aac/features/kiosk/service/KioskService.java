package com.tmc.aac.features.kiosk.service;

import com.tmc.aac.features.kiosk.repository.*;
import com.tmc.aac.features.kiosk.service.KioskViews.*;
import org.springframework.stereotype.Service;

@Service
public class KioskService {
    private final AccessTransactionCoordinator coordinator;
    private final AccessTransactionStore transactions;
    private final OutboxStore outbox;
    private final AccessCachePort cache;
    private final DeviceConfigurationPort configuration;
    private final SyncAgent sync;
    public KioskService(AccessTransactionCoordinator coordinator, AccessTransactionStore transactions,
            OutboxStore outbox, AccessCachePort cache, DeviceConfigurationPort configuration, SyncAgent sync) {
        this.coordinator = coordinator; this.transactions = transactions; this.outbox = outbox;
        this.cache = cache; this.configuration = configuration; this.sync = sync;
    }
    public TransactionView scan(String scanId, String payload) { return TransactionView.from(coordinator.scan(scanId, payload)); }
    public StatusView status() {
        var device = configuration.device();
        return new StatusView(device.kioskId(), device.factoryId(), device.gateId(), device.direction().name(),
                outbox.pendingCount(), sync.connected(), cache.cacheVersion(), transactions.recent().stream().map(TransactionView::from).toList());
    }
}
