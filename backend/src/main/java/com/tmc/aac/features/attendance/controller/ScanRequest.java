package com.tmc.aac.features.attendance.controller;

import jakarta.validation.constraints.NotBlank;

public record ScanRequest(
		@NotBlank String qrPayload,
		@NotBlank String gateCode
) {
}
