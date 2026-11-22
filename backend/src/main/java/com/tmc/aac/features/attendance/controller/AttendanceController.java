package com.tmc.aac.features.attendance.controller;

import com.tmc.aac.features.attendance.service.AttendanceService;
import com.tmc.aac.features.attendance.domain.AttendanceEvent;
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

@RestController
@RequestMapping("/api/v1/attendance")
@Tag(name = "Attendance", description = "Attendance event recording (scaffold)")
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
	@Operation(summary = "Record a QR scan at a gate (not implemented yet)")
	public ApiResponse<AttendanceEvent> scan(@Valid @RequestBody ScanRequest request) {
		return ApiResponse.ok(attendanceService.recordScan(request.qrPayload(), request.gateCode()));
	}
}
