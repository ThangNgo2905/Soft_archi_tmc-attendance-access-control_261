package com.tmc.aac.features.kiosk.service;

import com.tmc.aac.features.kiosk.domain.DeviceIdentity;
import com.tmc.aac.features.kiosk.domain.IntegrationContracts.*;
import com.tmc.aac.features.kiosk.repository.*;
import java.time.Clock;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class SyncAgent {
    private final OutboxStore outbox;
    private final AccessCachePort cache;
    private final KioskSyncPort remote;
    private final DeviceIdentity device;
    private final Clock clock;
    private volatile boolean connected;
    public SyncAgent(OutboxStore outbox, AccessCachePort cache, KioskSyncPort remote,
            DeviceConfigurationPort configuration, Clock clock) {
        this.outbox = outbox; this.cache = cache; this.remote = remote; device = configuration.device(); this.clock = clock;
    }
    public boolean connected() { return connected; }

    public synchronized void synchronize() {
        var pending = outbox.due(clock.instant());
        Set<String> acknowledged = Set.of();
        try {
            if (!pending.isEmpty()) {
                var ack = remote.send(new Batch(pending.stream().map(OutboxStore.PendingEvent::event).toList()));
                if (ack.acknowledgedEventIds() != null) acknowledged = new HashSet<>(ack.acknowledgedEventIds());
            }
            connected = true;
        } catch (RemoteUnavailable ex) { connected = false; }
        for (var item : pending) {
            if (acknowledged.contains(item.event().eventId())) outbox.acknowledge(item.event().eventId());
            else {
                long seconds = Math.min(60, 1L << Math.min(item.attempts(), 6));
                outbox.retry(item.event().eventId(), clock.instant().plusSeconds(seconds));
            }
        }
        try {
            remote.heartbeat(new Heartbeat(device.kioskId(), device.factoryId(), device.gateId(), device.direction(), outbox.pendingCount(), clock.instant()));
            cache.replaceCache(remote.cache(device.factoryId()));
            connected = true;
        } catch (RemoteUnavailable ex) { connected = false; }
    }
}
