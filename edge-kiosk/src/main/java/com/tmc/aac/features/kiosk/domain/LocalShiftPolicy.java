package com.tmc.aac.features.kiosk.domain;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;

public final class LocalShiftPolicy {
    public void verify(EmployeeAccess employee, Instant now, ZoneId zone) {
        LocalTime time = now.atZone(zone).toLocalTime();
        LocalTime start = employee.shiftStart(), end = employee.shiftEnd();
        boolean allowed = !start.equals(end) && (start.isBefore(end)
                ? !time.isBefore(start) && time.isBefore(end)
                : !time.isBefore(start) || time.isBefore(end));
        if (!allowed) throw new AccessRejected("OUTSIDE_SHIFT");
    }
}
