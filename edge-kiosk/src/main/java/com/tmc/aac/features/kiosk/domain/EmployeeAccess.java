package com.tmc.aac.features.kiosk.domain;

import java.time.LocalTime;

public record EmployeeAccess(String employeeId, String factoryId, String keyId, String secret,
                             boolean active, LocalTime shiftStart, LocalTime shiftEnd) {}
