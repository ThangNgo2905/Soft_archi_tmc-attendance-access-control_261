package com.tmc.aac.features.kiosk.domain;

import java.time.ZoneId;

public record DeviceIdentity(String kioskId, String factoryId, String gateId, Direction direction, ZoneId zone) {}
