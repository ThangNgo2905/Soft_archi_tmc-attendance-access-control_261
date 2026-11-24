package com.tmc.aac.features.attendance.controller;

import com.tmc.aac.features.attendance.domain.AttendanceDirection;
import com.tmc.aac.features.attendance.domain.AttendanceStatus;
import com.tmc.aac.features.attendance.domain.ScanResult;
import com.tmc.aac.features.attendance.service.AttendanceService;
import com.tmc.aac.shared.exception.ConflictException;
import com.tmc.aac.shared.exception.GlobalExceptionHandler;
import com.tmc.aac.shared.exception.InvalidQrException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AttendanceControllerTest {

	private AttendanceService attendanceService;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		attendanceService = mock(AttendanceService.class);
		mockMvc = MockMvcBuilders
				.standaloneSetup(new AttendanceController(attendanceService))
				.setControllerAdvice(new GlobalExceptionHandler())
				.build();
	}

	@Test
	void postApiAttendanceScanReturnsGrantedResult() throws Exception {
		ScanResult expected = new ScanResult(
				"tx-100",
				"EMP001",
				"Nguyen Van An",
				"GATE_BD_IN_01",
				"Binh Duong",
				AttendanceDirection.IN,
				AttendanceStatus.GRANTED,
				true,
				Instant.parse("2026-11-24T08:00:00Z")
		);
		when(attendanceService.recordScan(any(ScanRequest.class))).thenReturn(expected);

		mockMvc.perform(post("/api/attendance/scan")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"qrPayload\":\"{}\",\"gateCode\":\"GATE_BD_IN_01\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.transactionId").value("tx-100"))
				.andExpect(jsonPath("$.data.turnstileOpened").value(true));
	}

	@Test
	void postApiV1AttendanceScanAlsoWorks() throws Exception {
		ScanResult expected = new ScanResult(
				"tx-101",
				"EMP001",
				"Nguyen Van An",
				"GATE_BD_IN_01",
				"Binh Duong",
				AttendanceDirection.IN,
				AttendanceStatus.GRANTED,
				true,
				Instant.parse("2026-11-24T08:00:00Z")
		);
		when(attendanceService.recordScan(any(ScanRequest.class))).thenReturn(expected);

		mockMvc.perform(post("/api/v1/attendance/scan")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"qrPayload\":\"{}\",\"gateCode\":\"GATE_BD_IN_01\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.employeeCode").value("EMP001"));
	}

	@Test
	void invalidQrReturns401() throws Exception {
		when(attendanceService.recordScan(any(ScanRequest.class)))
				.thenThrow(new InvalidQrException("QR_EXPIRED: Dynamic QR token has expired"));

		mockMvc.perform(post("/api/attendance/scan")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"qrPayload\":\"bad\",\"gateCode\":\"GATE_BD_IN_01\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.success").value(false));
	}

	@Test
	void antiPassbackReturns409() throws Exception {
		when(attendanceService.recordScan(any(ScanRequest.class)))
				.thenThrow(new ConflictException("ANTI_PASSBACK_VIOLATION: Consecutive IN scan is not allowed"));

		mockMvc.perform(post("/api/attendance/scan")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"qrPayload\":\"{}\",\"gateCode\":\"GATE_BD_IN_01\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.success").value(false));
	}
}
