package com.tmc.aac.features.attendance.domain;

import java.time.Instant;

public record ScanResult(
		String transactionId,
		String employeeCode,
		String employeeName,
		String gateCode,
		String factoryCode,
		AttendanceDirection direction,
		AttendanceStatus status,
		boolean turnstileOpened,
		Instant occurredAt
) {
}
