package com.tmc.aac.features.attendance.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tmc.aac.features.attendance.controller.ScanRequest;
import com.tmc.aac.features.attendance.domain.AttendanceDirection;
import com.tmc.aac.features.attendance.domain.AttendanceEvent;
import com.tmc.aac.features.attendance.domain.AttendanceStatus;
import com.tmc.aac.features.attendance.domain.ParsedQrToken;
import com.tmc.aac.features.attendance.domain.QrTokenService;
import com.tmc.aac.features.attendance.domain.ScanResult;
import com.tmc.aac.features.attendance.repository.AttendanceRepositoryPort;
import com.tmc.aac.features.gate.domain.GateDirection;
import com.tmc.aac.features.gate.domain.GateStation;
import com.tmc.aac.features.gate.repository.GateRepositoryPort;
import com.tmc.aac.features.integration.repository.TurnstilePort;
import com.tmc.aac.features.user.domain.Employee;
import com.tmc.aac.features.user.repository.EmployeeRepositoryPort;
import com.tmc.aac.shared.exception.BadRequestException;
import com.tmc.aac.shared.exception.ConflictException;
import com.tmc.aac.shared.exception.ExternalServiceException;
import com.tmc.aac.shared.exception.ResourceNotFoundException;

@Service
public class AttendanceService {

	private final AttendanceRepositoryPort attendanceRepository;
	private final EmployeeRepositoryPort employeeRepository;
	private final GateRepositoryPort gateRepository;
	private final TurnstilePort turnstilePort;
	private final QrTokenService qrTokenService;
	private final boolean antiPassbackEnabled;
	private final Duration maxOfflineAge;
	private final Duration clockSkew;
	private final Clock clock;

	@Autowired
	public AttendanceService(
			AttendanceRepositoryPort attendanceRepository,
			EmployeeRepositoryPort employeeRepository,
			GateRepositoryPort gateRepository,
			TurnstilePort turnstilePort,
			@Value("${tmc.qr.hmac-secret}") String hmacSecret,
			@Value("${tmc.qr.ttl-seconds:30}") long ttlSeconds,
			@Value("${tmc.qr.clock-skew-seconds:5}") long clockSkewSeconds,
			@Value("${tmc.qr.allow-unsigned-dev:false}") boolean allowUnsignedDev,
			@Value("${tmc.attendance.anti-passback-enabled:true}") boolean antiPassbackEnabled,
			@Value("${tmc.attendance.max-offline-age-hours:24}") long maxOfflineAgeHours) {
		this(
				attendanceRepository,
				employeeRepository,
				gateRepository,
				turnstilePort,
				new QrTokenService(hmacSecret, ttlSeconds, clockSkewSeconds, allowUnsignedDev),
				antiPassbackEnabled,
				Duration.ofHours(maxOfflineAgeHours),
				Duration.ofSeconds(clockSkewSeconds),
				Clock.systemUTC()
		);
	}

	public AttendanceService(
			AttendanceRepositoryPort attendanceRepository,
			EmployeeRepositoryPort employeeRepository,
			GateRepositoryPort gateRepository,
			TurnstilePort turnstilePort,
			QrTokenService qrTokenService,
			boolean antiPassbackEnabled,
			Duration maxOfflineAge,
			Duration clockSkew,
			Clock clock) {
		this.attendanceRepository = attendanceRepository;
		this.employeeRepository = employeeRepository;
		this.gateRepository = gateRepository;
		this.turnstilePort = turnstilePort;
		this.qrTokenService = qrTokenService;
		this.antiPassbackEnabled = antiPassbackEnabled;
		this.maxOfflineAge = maxOfflineAge;
		this.clockSkew = clockSkew;
		this.clock = clock;
	}

	public List<AttendanceEvent> listRecent(int limit) {
		return attendanceRepository.findRecent(limit);
	}

	@Transactional
	public ScanResult recordScan(ScanRequest request) {
		String transactionId = resolveTransactionId(request.transactionId());

		Optional<AttendanceEvent> existing = attendanceRepository.findByTransactionId(transactionId);
		if (existing.isPresent()) {
			return toScanResult(existing.get(), false);
		}

		boolean isOfflineSync = Boolean.TRUE.equals(request.offlineSync());
		// Online scans are timed by the server clock; only offline sync may carry a client timestamp,
		// and that timestamp must fall inside the allowed offline window.
		Instant occurredAt = isOfflineSync
				? validateOfflineTimestamp(request.occurredAt())
				: Instant.now(clock);

		ParsedQrToken token = qrTokenService.parseAndValidate(request.qrPayload(), occurredAt);

		if (token.nonce() != null && attendanceRepository.existsByNonce(token.nonce())) {
			throw new ConflictException("REPLAY_NONCE_DETECTED: Dynamic QR token has already been used");
		}

		Employee employee = employeeRepository.findByEmployeeCode(token.employeeCode())
				.orElseThrow(() -> new ResourceNotFoundException("Employee", token.employeeCode()));
		if (!employee.isActive()) {
			throw new ConflictException("EMPLOYEE_INACTIVE: Employee is not active");
		}

		GateStation gate = gateRepository.findByCode(request.gateCode().trim())
				.orElseThrow(() -> new ResourceNotFoundException("GateStation", request.gateCode()));
		if (!gate.isActive()) {
			throw new ConflictException("GATE_INACTIVE: Gate station is not active");
		}

		AttendanceDirection direction = mapDirection(gate.getDirection());

		if (antiPassbackEnabled && !isOfflineSync) {
			enforceAntiPassback(employee.getId(), direction);
		}

		AttendanceEvent event = new AttendanceEvent();
		event.setId(UUID.randomUUID());
		event.setTransactionId(transactionId);
		event.setEmployeeId(employee.getId());
		event.setGateId(gate.getId());
		event.setDirection(direction);
		event.setNonce(token.nonce());
		event.setStatus(isOfflineSync ? AttendanceStatus.SYNCED_OFFLINE : AttendanceStatus.GRANTED);
		event.setOccurredAt(occurredAt);

		// Persist (and flush) first: a concurrent replay of the same nonce fails here on the UNIQUE
		// constraint, before the turnstile is touched. If unlocking fails afterwards, the exception
		// rolls back this transaction so no GRANTED row is left without an opened gate.
		AttendanceEvent saved = attendanceRepository.save(event);

		boolean turnstileOpened = false;
		if (!isOfflineSync) {
			turnstileOpened = unlockTurnstile(gate.getCode(), transactionId);
		}

		return new ScanResult(
				saved.getTransactionId(),
				employee.getEmployeeCode(),
				employee.getFullName(),
				gate.getCode(),
				gate.getFactoryCode(),
				saved.getDirection(),
				saved.getStatus(),
				turnstileOpened,
				saved.getOccurredAt()
		);
	}

	public String generateSampleQr(String employeeCode) {
		Employee employee = employeeRepository.findByEmployeeCode(employeeCode.trim())
				.orElseThrow(() -> new ResourceNotFoundException("Employee", employeeCode));
		return qrTokenService.generateDynamicQrPayload(
				employee.getEmployeeCode(),
				Instant.now(clock),
				UUID.randomUUID().toString()
		);
	}

	private Instant validateOfflineTimestamp(Instant occurredAt) {
		if (occurredAt == null) {
			throw new BadRequestException("OFFLINE_SYNC_TIMESTAMP_REQUIRED: scannedAt is required for offline sync");
		}
		Instant now = Instant.now(clock);
		if (occurredAt.isAfter(now.plus(clockSkew))) {
			throw new BadRequestException("OFFLINE_SYNC_FUTURE_TIMESTAMP: scannedAt is in the future");
		}
		if (occurredAt.isBefore(now.minus(maxOfflineAge))) {
			throw new BadRequestException("OFFLINE_SYNC_TOO_OLD: scannedAt is older than the allowed offline window of "
					+ maxOfflineAge.toHours() + "h");
		}
		return occurredAt;
	}

	private void enforceAntiPassback(UUID employeeId, AttendanceDirection nextDirection) {
		Optional<AttendanceEvent> latest = attendanceRepository.findLatestByEmployeeId(employeeId);
		if (latest.isPresent() && latest.get().getDirection() == nextDirection) {
			throw new ConflictException(
					"ANTI_PASSBACK_VIOLATION: Consecutive " + nextDirection + " scan is not allowed");
		}
	}

	private boolean unlockTurnstile(String gateCode, String transactionId) {
		try {
			boolean unlocked = turnstilePort.unlock(gateCode, transactionId);
			if (!unlocked) {
				throw new ExternalServiceException(
						"TURNSTILE_UNLOCK_REJECTED: Turnstile controller refused to unlock gate " + gateCode);
			}
			return true;
		} catch (ExternalServiceException ex) {
			throw ex;
		} catch (RuntimeException ex) {
			throw new ExternalServiceException(
					"TURNSTILE_UNREACHABLE: Failed to reach turnstile controller for gate " + gateCode);
		}
	}

	private ScanResult toScanResult(AttendanceEvent event, boolean turnstileOpened) {
		Employee employee = employeeRepository.findById(event.getEmployeeId()).orElse(null);
		GateStation gate = gateRepository.findById(event.getGateId()).orElse(null);
		return new ScanResult(
				event.getTransactionId(),
				employee != null ? employee.getEmployeeCode() : null,
				employee != null ? employee.getFullName() : null,
				gate != null ? gate.getCode() : null,
				gate != null ? gate.getFactoryCode() : null,
				event.getDirection(),
				event.getStatus() != null ? event.getStatus() : AttendanceStatus.GRANTED,
				turnstileOpened,
				event.getOccurredAt()
		);
	}

	private static AttendanceDirection mapDirection(GateDirection gateDirection) {
		return gateDirection == GateDirection.OUT ? AttendanceDirection.OUT : AttendanceDirection.IN;
	}

	private static String resolveTransactionId(String rawTransactionId) {
		if (rawTransactionId != null && !rawTransactionId.isBlank()) {
			return rawTransactionId.trim();
		}
		return UUID.randomUUID().toString();
	}
}
