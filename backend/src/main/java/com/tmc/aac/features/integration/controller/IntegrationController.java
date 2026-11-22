package com.tmc.aac.features.integration.controller;

import com.tmc.aac.features.integration.service.IntegrationService;
import com.tmc.aac.shared.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/integration")
@Tag(name = "Integration", description = "HR and turnstile adapters (scaffold)")
public class IntegrationController {

	private final IntegrationService integrationService;

	public IntegrationController(IntegrationService integrationService) {
		this.integrationService = integrationService;
	}

	@PostMapping("/turnstile/unlock")
	@Operation(summary = "Proxy unlock call to turnstile mock/API")
	public ApiResponse<Map<String, Object>> unlock(@RequestBody Map<String, String> request) {
		boolean ok = integrationService.unlockGate(
				request.get("gateId"),
				request.get("transactionId"));
		return ApiResponse.ok(Map.of("unlocked", ok));
	}

	@PostMapping("/hr/sync")
	@Operation(summary = "Push attendance batch to HR mock/API")
	public ApiResponse<Map<String, Object>> syncHr(@RequestBody Map<String, List<Map<String, Object>>> request) {
		List<Map<String, Object>> records = request.getOrDefault("records", List.of());
		int count = integrationService.pushAttendanceToHr(records);
		return ApiResponse.ok(Map.of("count", count));
	}
}
