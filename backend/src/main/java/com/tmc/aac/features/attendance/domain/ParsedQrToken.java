package com.tmc.aac.features.attendance.domain;

public record ParsedQrToken(
		String employeeCode,
		Long timestampMillis,
		String nonce,
		String signature
) {
}
