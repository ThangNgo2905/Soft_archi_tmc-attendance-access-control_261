package com.tmc.aac.features.attendance.controller;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public record ScanRequest(
		@NotBlank String qrPayload,
		@NotBlank @JsonAlias("gateId") String gateCode,
		String transactionId,
		@JsonAlias("scannedAt") Instant occurredAt,
		Boolean offlineSync
) {

	public ScanRequest(String qrPayload, String gateCode) {
		this(qrPayload, gateCode, null, null, false);
	}
}
