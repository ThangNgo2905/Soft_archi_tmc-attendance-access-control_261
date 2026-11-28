package com.tmc.aac.features.attendance.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataAttendanceJpaRepository extends JpaRepository<AttendanceEventJpaEntity, UUID> {

	List<AttendanceEventJpaEntity> findAllByOrderByOccurredAtDesc(Pageable pageable);

	boolean existsByNonce(String nonce);

	Optional<AttendanceEventJpaEntity> findByTransactionId(String transactionId);

	Optional<AttendanceEventJpaEntity> findFirstByEmployeeIdOrderByOccurredAtDesc(UUID employeeId);
}
