package com.tmc.aac.features.reporting.controller;

import com.tmc.aac.features.reporting.service.ReportingService;
import com.tmc.aac.shared.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Reports", description = "Reporting and auditing (scaffold)")
public class ReportingController {

	private final ReportingService reportingService;

	public ReportingController(ReportingService reportingService) {
		this.reportingService = reportingService;
	}

	@GetMapping("/monthly")
	@Operation(summary = "Monthly attendance summary (not implemented yet)")
	public ApiResponse<Map<String, Object>> monthly(
			@RequestParam(defaultValue = "2026-11") String yearMonth) {
		return ApiResponse.ok(reportingService.monthlySummary(yearMonth));
	}
}
