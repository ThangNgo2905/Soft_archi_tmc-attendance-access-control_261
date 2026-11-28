package com.tmc.aac.features.attendance.repository;

import com.tmc.aac.features.attendance.domain.AttendanceEvent;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttendanceRepositoryPort {

	List<AttendanceEvent> findRecent(int limit);

	/**
	 * Persists the event immediately (not deferred to commit), so uniqueness violations surface
	 * before any side effect such as unlocking a turnstile.
	 *
	 * @throws com.tmc.aac.shared.exception.ConflictException if the nonce or transaction id already exists
	 */
	AttendanceEvent save(AttendanceEvent event);

	boolean existsByNonce(String nonce);

	Optional<AttendanceEvent> findByTransactionId(String transactionId);

	Optional<AttendanceEvent> findLatestByEmployeeId(UUID employeeId);
}
