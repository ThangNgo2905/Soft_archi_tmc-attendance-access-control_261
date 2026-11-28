package com.tmc.aac.features.kiosk.repository;

import com.tmc.aac.features.kiosk.domain.DeviceIdentity;
import com.tmc.aac.features.kiosk.domain.Direction;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;

@Component
@DependsOn("sqliteSchema")
public class LocalDeviceConfiguration implements DeviceConfigurationPort {
    private final DeviceIdentity device;
    public LocalDeviceConfiguration(@Value("${edge.kiosk-id}") String kiosk,
            @Value("${edge.factory-id}") String factory, @Value("${edge.gate-id}") String gate,
            @Value("${edge.direction}") String direction, @Value("${edge.zone}") String zone, JdbcTemplate jdbc) {
        device = new DeviceIdentity(kiosk, factory, gate, Direction.valueOf(direction), ZoneId.of(zone));
        String identity = String.join("|", kiosk, factory, gate, direction, zone);
        jdbc.update("INSERT OR IGNORE INTO device_configuration(id,identity) VALUES(1,?)", identity);
        String stored = jdbc.queryForObject("SELECT identity FROM device_configuration WHERE id=1", String.class);
        if (!identity.equals(stored)) throw new IllegalStateException("Database belongs to a different kiosk configuration; use a separate data file");
    }
    @Override public DeviceIdentity device() { return device; }
}
