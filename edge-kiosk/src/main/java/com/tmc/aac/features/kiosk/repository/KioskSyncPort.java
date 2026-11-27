package com.tmc.aac.features.kiosk.repository;
import com.tmc.aac.features.kiosk.domain.IntegrationContracts.*;
public interface KioskSyncPort {
    BatchAck send(Batch batch);
    CacheSnapshot cache(String factoryId);
    void heartbeat(Heartbeat heartbeat);
}
