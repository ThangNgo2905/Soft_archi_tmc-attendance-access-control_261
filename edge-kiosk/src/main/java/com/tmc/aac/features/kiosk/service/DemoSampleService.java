package com.tmc.aac.features.kiosk.service;

import com.tmc.aac.features.kiosk.domain.*;
import com.tmc.aac.features.kiosk.domain.IntegrationContracts.CacheSnapshot;
import com.tmc.aac.features.kiosk.repository.AccessCachePort;
import com.tmc.aac.features.kiosk.repository.DeviceConfigurationPort;
import com.tmc.aac.features.kiosk.service.KioskViews.SampleView;
import jakarta.annotation.PostConstruct;
import java.time.Clock;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("demo")
public class DemoSampleService {
    private final AccessCachePort cache;
    private final DeviceConfigurationPort configuration;
    private final Clock clock;
    public DemoSampleService(AccessCachePort cache, DeviceConfigurationPort configuration, Clock clock) {
        this.cache = cache; this.configuration = configuration; this.clock = clock;
    }
    @PostConstruct
    void seed() {
        String factory = configuration.device().factoryId();
        cache.replaceCache(new CacheSnapshot(0, List.of(
                employee("DEMO-001", factory, true, LocalTime.MIN, LocalTime.MAX),
                employee("DEMO-002", factory, true, LocalTime.MIN, LocalTime.MIN),
                employee("DEMO-003", factory, false, LocalTime.MIN, LocalTime.MAX),
                employee("DEMO-004", factory, true, LocalTime.MIN, LocalTime.MAX),
                employee("DEMO-OTHER", "OTHER", true, LocalTime.MIN, LocalTime.MAX))));
    }
    private EmployeeAccess employee(String id, String factory, boolean active, LocalTime start, LocalTime end) {
        return new EmployeeAccess(id, factory, "demo-key-v1", "demo-only-not-a-production-secret", active, start, end);
    }
    public SampleView sample(String kind) {
        String employeeId = switch (kind) {
            case "VALID", "EXPIRED", "BAD_SIGNATURE" -> "DEMO-001";
            case "OUTSIDE_SHIFT" -> "DEMO-002";
            case "INACTIVE" -> "DEMO-003";
            case "VALID_SECOND_EMPLOYEE" -> "DEMO-004";
            case "WRONG_FACTORY" -> "DEMO-OTHER";
            default -> throw new IllegalArgumentException("Unknown sample type");
        };
        var employee = cache.employee(employeeId).orElseThrow(() -> new IllegalArgumentException("Demo employee unavailable"));
        long issued = clock.instant().getEpochSecond() - (kind.equals("EXPIRED") ? 60 : 0);
        var token = new QrToken(employeeId, issued, issued + 30, UUID.randomUUID().toString(), employee.keyId(), "");
        return new SampleView(kind, token.sign(kind.equals("BAD_SIGNATURE") ? "incorrect-demo-key" : employee.secret()).encode());
    }
}
