package com.tmc.aac.features.attendance.service;

import com.tmc.aac.features.attendance.controller.ScanRequest;
import com.tmc.aac.features.attendance.domain.AttendanceDirection;
import com.tmc.aac.features.attendance.domain.AttendanceStatus;
import com.tmc.aac.features.attendance.domain.QrTokenService;
import com.tmc.aac.features.attendance.domain.ScanResult;
import com.tmc.aac.features.attendance.repository.InMemoryAttendanceRepository;
import com.tmc.aac.features.gate.domain.GateDirection;
import com.tmc.aac.features.gate.domain.GateStation;
import com.tmc.aac.features.gate.repository.InMemoryGateRepository;
import com.tmc.aac.features.integration.repository.TurnstilePort;
import com.tmc.aac.features.user.domain.Employee;
import com.tmc.aac.features.user.repository.InMemoryEmployeeRepository;
import com.tmc.aac.features.attendance.domain.AttendanceEvent;
import com.tmc.aac.shared.exception.BadRequestException;
import com.tmc.aac.shared.exception.ConflictException;
import com.tmc.aac.shared.exception.ExternalServiceException;
import com.tmc.aac.shared.exception.InvalidQrException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttendanceServiceTest {

	private static final Instant FIXED_NOW = Instant.parse("2026-11-24T08:00:00Z");

	private InMemoryAttendanceRepository attendanceRepo;
	private InMemoryEmployeeRepository employeeRepo;
	private InMemoryGateRepository gateRepo;
	private StubTurnstilePort turnstilePort;
	private QrTokenService qrTokenService;
	private AttendanceService attendanceService;

	@BeforeEach
	void setUp() {
		attendanceRepo = new InMemoryAttendanceRepository();
		employeeRepo = new InMemoryEmployeeRepository();
		gateRepo = new InMemoryGateRepository();
		turnstilePort = new StubTurnstilePort();
		qrTokenService = new QrTokenService("unit-test-secret", 30, 5, false);

		Employee emp = new Employee();
		emp.setId(UUID.randomUUID());
		emp.setEmployeeCode("EMP001");
		emp.setFullName("Nguyen Van An");
		emp.setFactoryCode("Binh Duong");
		emp.setActive(true);
		employeeRepo.save(emp);

		GateStation gateInA = new GateStation();
		gateInA.setId(UUID.randomUUID());
		gateInA.setCode("GATE_BD_IN_01");
		gateInA.setName("Cong Vao 01");
		gateInA.setFactoryCode("Binh Duong");
		gateInA.setDirection(GateDirection.IN);
		gateInA.setActive(true);
		gateRepo.save(gateInA);

		GateStation gateInB = new GateStation();
		gateInB.setId(UUID.randomUUID());
		gateInB.setCode("GATE_BD_IN_02");
		gateInB.setName("Cong Vao 02");
		gateInB.setFactoryCode("Binh Duong");
		gateInB.setDirection(GateDirection.IN);
		gateInB.setActive(true);
		gateRepo.save(gateInB);

		GateStation gateOut = new GateStation();
		gateOut.setId(UUID.randomUUID());
		gateOut.setCode("GATE_BD_OUT_01");
		gateOut.setName("Cong Ra 01");
		gateOut.setFactoryCode("Binh Duong");
		gateOut.setDirection(GateDirection.OUT);
		gateOut.setActive(true);
		gateRepo.save(gateOut);

		attendanceService = newService(attendanceRepo);
	}

	private AttendanceService newService(InMemoryAttendanceRepository repo) {
		return new AttendanceService(
				repo,
				employeeRepo,
				gateRepo,
				turnstilePort,
				qrTokenService,
				true,
				Duration.ofHours(24),
				Duration.ofSeconds(5),
				Clock.fixed(FIXED_NOW, ZoneOffset.UTC)
		);
	}

	@Test
	void recordScanUnlocksTurnstileAndPersistsEvent() {
		String qr = qrTokenService.generateDynamicQrPayload("EMP001", FIXED_NOW, "nonce-1");

		ScanResult result = attendanceService.recordScan(new ScanRequest(qr, "GATE_BD_IN_01"));

		assertEquals("EMP001", result.employeeCode());
		assertEquals("GATE_BD_IN_01", result.gateCode());
		assertEquals(AttendanceDirection.IN, result.direction());
		assertEquals(AttendanceStatus.GRANTED, result.status());
		assertTrue(result.turnstileOpened());
		assertEquals(1, turnstilePort.calls.size());
		assertEquals(1, attendanceRepo.findRecent(10).size());
	}

	@Test
	void rejectsReplayAttackWithSameNonce() {
		String qr = qrTokenService.generateDynamicQrPayload("EMP001", FIXED_NOW, "nonce-replay");
		attendanceService.recordScan(new ScanRequest(qr, "GATE_BD_IN_01"));

		ConflictException ex = assertThrows(
				ConflictException.class,
				() -> attendanceService.recordScan(new ScanRequest(qr, "GATE_BD_OUT_01"))
		);
		assertTrue(ex.getMessage().contains("REPLAY_NONCE_DETECTED"));
		assertEquals(1, turnstilePort.calls.size());
	}

	@Test
	void enforcesAntiPassbackAndAllowsValidExit() {
		String qrIn = qrTokenService.generateDynamicQrPayload("EMP001", FIXED_NOW, "nonce-in");
		attendanceService.recordScan(new ScanRequest(qrIn, "GATE_BD_IN_01"));

		String qrSecondIn = qrTokenService.generateDynamicQrPayload("EMP001", FIXED_NOW, "nonce-in-2");
		ConflictException ex = assertThrows(
				ConflictException.class,
				() -> attendanceService.recordScan(new ScanRequest(qrSecondIn, "GATE_BD_IN_02"))
		);
		assertTrue(ex.getMessage().contains("ANTI_PASSBACK_VIOLATION"));

		String qrOut = qrTokenService.generateDynamicQrPayload("EMP001", FIXED_NOW, "nonce-out");
		ScanResult outResult = attendanceService.recordScan(new ScanRequest(qrOut, "GATE_BD_OUT_01"));
		assertEquals(AttendanceDirection.OUT, outResult.direction());
		assertEquals(2, attendanceRepo.findRecent(10).size());
	}

	@Test
	void persistsBeforeUnlockingSoTurnstileFailureRollsBack() throws NoSuchMethodException {
		turnstilePort.shouldSucceed = false;
		turnstilePort.repoToInspect = attendanceRepo;
		String qr = qrTokenService.generateDynamicQrPayload("EMP001", FIXED_NOW, "nonce-fail");

		assertThrows(
				ExternalServiceException.class,
				() -> attendanceService.recordScan(new ScanRequest(qr, "GATE_BD_IN_01"))
		);
		// Event was written (and flushed) before the unlock call ...
		assertEquals(1, turnstilePort.persistedCountAtUnlock);
		// ... and the unchecked ExternalServiceException rolls it back via @Transactional in production.
		Transactional tx = AttendanceService.class
				.getMethod("recordScan", ScanRequest.class)
				.getAnnotation(Transactional.class);
		assertNotNull(tx);
	}

	@Test
	void concurrentReplayFailsOnSaveWithoutSecondUnlock() {
		// Simulates the race: both requests pass existsByNonce, only the UNIQUE constraint stops the second.
		InMemoryAttendanceRepository racyRepo = new InMemoryAttendanceRepository() {
			@Override
			public boolean existsByNonce(String nonce) {
				return false;
			}
		};
		AttendanceService racyService = newService(racyRepo);
		String qr = qrTokenService.generateDynamicQrPayload("EMP001", FIXED_NOW, "nonce-race");

		racyService.recordScan(new ScanRequest(qr, "GATE_BD_IN_01"));
		ConflictException ex = assertThrows(
				ConflictException.class,
				() -> racyService.recordScan(new ScanRequest(qr, "GATE_BD_OUT_01"))
		);
		assertTrue(ex.getMessage().contains("REPLAY_NONCE_DETECTED"));
		assertEquals(1, turnstilePort.calls.size());
	}

	@Test
	void onlineScanUsesServerClockNotClientTimestamp() {
		// QR minted 10 minutes ago is expired; a client-supplied old timestamp must not revive it.
		Instant old = FIXED_NOW.minusSeconds(600);
		String qr = qrTokenService.generateDynamicQrPayload("EMP001", old, "nonce-old");

		assertThrows(
				InvalidQrException.class,
				() -> attendanceService.recordScan(new ScanRequest(qr, "GATE_BD_IN_01", "tx-old", old, false))
		);
		assertEquals(0, turnstilePort.calls.size());
	}

	@Test
	void offlineSyncRejectsTooOldTimestamp() {
		Instant tooOld = FIXED_NOW.minus(Duration.ofHours(25));
		String qr = qrTokenService.generateDynamicQrPayload("EMP001", tooOld, "nonce-too-old");

		BadRequestException ex = assertThrows(
				BadRequestException.class,
				() -> attendanceService.recordScan(new ScanRequest(qr, "GATE_BD_IN_01", "tx-too-old", tooOld, true))
		);
		assertTrue(ex.getMessage().contains("OFFLINE_SYNC_TOO_OLD"));
		assertEquals(0, attendanceRepo.findRecent(10).size());
	}

	@Test
	void offlineSyncRejectsFutureTimestamp() {
		Instant future = FIXED_NOW.plusSeconds(60);
		String qr = qrTokenService.generateDynamicQrPayload("EMP001", future, "nonce-future");

		BadRequestException ex = assertThrows(
				BadRequestException.class,
				() -> attendanceService.recordScan(new ScanRequest(qr, "GATE_BD_IN_01", "tx-future", future, true))
		);
		assertTrue(ex.getMessage().contains("OFFLINE_SYNC_FUTURE_TIMESTAMP"));
	}

	@Test
	void offlineSyncRequiresTimestamp() {
		String qr = qrTokenService.generateDynamicQrPayload("EMP001", FIXED_NOW, "nonce-no-ts");

		BadRequestException ex = assertThrows(
				BadRequestException.class,
				() -> attendanceService.recordScan(new ScanRequest(qr, "GATE_BD_IN_01", "tx-no-ts", null, true))
		);
		assertTrue(ex.getMessage().contains("OFFLINE_SYNC_TIMESTAMP_REQUIRED"));
	}

	@Test
	void syncsOfflineScanIdempotentlyWithoutReunlockingTurnstile() {
		Instant offlineScannedAt = FIXED_NOW.minusSeconds(600);
		String qr = qrTokenService.generateDynamicQrPayload("EMP001", offlineScannedAt, "nonce-offline");
		ScanRequest syncReq = new ScanRequest(qr, "GATE_BD_IN_01", "tx-offline-1", offlineScannedAt, true);

		ScanResult first = attendanceService.recordScan(syncReq);
		ScanResult duplicate = attendanceService.recordScan(syncReq);

		assertEquals("tx-offline-1", first.transactionId());
		assertEquals("tx-offline-1", duplicate.transactionId());
		assertFalse(first.turnstileOpened());
		assertEquals(AttendanceStatus.SYNCED_OFFLINE, first.status());
		assertEquals(0, turnstilePort.calls.size());
		assertEquals(1, attendanceRepo.findRecent(10).size());
	}

	private static class StubTurnstilePort implements TurnstilePort {
		boolean shouldSucceed = true;
		final List<String> calls = new ArrayList<>();
		InMemoryAttendanceRepository repoToInspect;
		int persistedCountAtUnlock = -1;

		@Override
		public boolean unlock(String gateId, String transactionId) {
			calls.add(gateId + ":" + transactionId);
			if (repoToInspect != null) {
				List<AttendanceEvent> persisted = repoToInspect.findRecent(10);
				persistedCountAtUnlock = persisted.size();
			}
			return shouldSucceed;
		}
	}
}
