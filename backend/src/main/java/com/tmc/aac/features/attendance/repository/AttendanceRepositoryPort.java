package com.tmc.aac.features.attendance.repository;

import com.tmc.aac.features.attendance.domain.AttendanceEvent;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttendanceRepositoryPort {

	List<AttendanceEvent> findRecent(int limit);

	AttendanceEvent save(AttendanceEvent event);

	boolean existsByNonce(String nonce);

	Optional<AttendanceEvent> findByTransactionId(String transactionId);

	Optional<AttendanceEvent> findLatestByEmployeeId(UUID employeeId);
}
