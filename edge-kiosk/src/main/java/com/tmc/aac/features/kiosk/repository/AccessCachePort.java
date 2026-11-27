package com.tmc.aac.features.kiosk.repository;
import com.tmc.aac.features.kiosk.domain.EmployeeAccess;
import com.tmc.aac.features.kiosk.domain.IntegrationContracts.CacheSnapshot;
import java.util.Optional;
public interface AccessCachePort {
    Optional<EmployeeAccess> employee(String id);
    void replaceCache(CacheSnapshot snapshot);
    long cacheVersion();
}
