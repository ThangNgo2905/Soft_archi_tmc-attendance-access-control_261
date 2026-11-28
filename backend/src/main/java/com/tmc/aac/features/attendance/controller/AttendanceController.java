package com.tmc.aac.features.attendance.controller;

import com.tmc.aac.features.attendance.domain.AttendanceEvent;
import com.tmc.aac.features.attendance.domain.ScanResult;
import com.tmc.aac.features.attendance.service.AttendanceService;
import com.tmc.aac.shared.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/v1/attendance", "/api/attendance"})
@Tag(name = "Attendance", description = "QR scan validation, turnstile unlock, and append-only attendance logging")
public class AttendanceController {

	private final AttendanceService attendanceService;

	public AttendanceController(AttendanceService attendanceService) {
		this.attendanceService = attendanceService;
	}

	@GetMapping("/events")
	@Operation(summary = "List recent attendance events")
	public ApiResponse<List<AttendanceEvent>> recent(
			@RequestParam(defaultValue = "50") int limit) {
		return ApiResponse.ok(attendanceService.listRecent(limit));
	}

	@PostMapping("/scan")
	@Operation(summary = "Validate dynamic QR, enforce anti-passback, unlock turnstile, and persist attendance")
	public ApiResponse<ScanResult> scan(@Valid @RequestBody ScanRequest request) {
		// Online path: never trust a client-supplied offline flag (it would skip anti-passback).
		ScanRequest onlineRequest = new ScanRequest(
				request.qrPayload(),
				request.gateCode(),
				request.transactionId(),
				request.occurredAt(),
				false
		);
		ScanResult result = attendanceService.recordScan(onlineRequest);
		return ApiResponse.ok("Attendance recorded and turnstile unlocked", result);
	}

	@PostMapping("/sync")
	@Operation(summary = "Sync offline Store-and-Forward attendance scan from Edge Kiosk")
	public ApiResponse<ScanResult> syncOffline(@Valid @RequestBody ScanRequest request) {
		ScanRequest syncRequest = new ScanRequest(
				request.qrPayload(),
				request.gateCode(),
				request.transactionId(),
				request.occurredAt(),
				true
		);
		ScanResult result = attendanceService.recordScan(syncRequest);
		return ApiResponse.ok("Offline attendance event synced", result);
	}

	@GetMapping("/qr-token")
	@Operation(summary = "Generate a signed Dynamic QR payload for an employee (FR-01 / testing)")
	public ApiResponse<Map<String, String>> generateQrToken(
			@RequestParam(defaultValue = "EMP001") String employeeCode) {
		String qrPayload = attendanceService.generateSampleQr(employeeCode);
		return ApiResponse.ok(Map.of("employeeCode", employeeCode, "qrPayload", qrPayload));
	}
}
