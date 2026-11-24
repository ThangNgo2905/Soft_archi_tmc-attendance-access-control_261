package com.tmc.aac.features.attendance.repository;

import com.tmc.aac.features.attendance.domain.AttendanceEvent;
import com.tmc.aac.features.attendance.domain.AttendanceStatus;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Append-only PostgreSQL adapter for attendance events.
 */
@Repository
@Primary
public class JpaAttendanceRepositoryAdapter implements AttendanceRepositoryPort {

	private final SpringDataAttendanceJpaRepository jpaRepository;

	public JpaAttendanceRepositoryAdapter(SpringDataAttendanceJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public List<AttendanceEvent> findRecent(int limit) {
		int safeLimit = Math.min(Math.max(limit, 1), 500);
		return jpaRepository.findAllByOrderByOccurredAtDesc(PageRequest.of(0, safeLimit)).stream()
				.map(this::toDomain)
				.toList();
	}

	@Override
	public AttendanceEvent save(AttendanceEvent event) {
		AttendanceEventJpaEntity entity = toEntity(event);
		AttendanceEventJpaEntity saved = jpaRepository.save(entity);
		return toDomain(saved);
	}

	@Override
	public boolean existsByNonce(String nonce) {
		return nonce != null && jpaRepository.existsByNonce(nonce);
	}

	@Override
	public Optional<AttendanceEvent> findByTransactionId(String transactionId) {
		return jpaRepository.findByTransactionId(transactionId).map(this::toDomain);
	}

	@Override
	public Optional<AttendanceEvent> findLatestByEmployeeId(UUID employeeId) {
		return jpaRepository.findFirstByEmployeeIdOrderByOccurredAtDesc(employeeId)
				.map(this::toDomain);
	}

	private AttendanceEvent toDomain(AttendanceEventJpaEntity entity) {
		AttendanceEvent domain = new AttendanceEvent();
		domain.setId(entity.getId());
		domain.setEmployeeId(entity.getEmployeeId());
		domain.setGateId(entity.getGateId());
		domain.setDirection(entity.getDirection());
		domain.setOccurredAt(entity.getOccurredAt());
		domain.setTransactionId(entity.getTransactionId());
		domain.setNonce(entity.getNonce());
		domain.setStatus(entity.getStatus());
		return domain;
	}

	private AttendanceEventJpaEntity toEntity(AttendanceEvent domain) {
		AttendanceEventJpaEntity entity = new AttendanceEventJpaEntity();
		entity.setId(domain.getId() != null ? domain.getId() : UUID.randomUUID());
		entity.setTransactionId(domain.getTransactionId());
		entity.setEmployeeId(domain.getEmployeeId());
		entity.setGateId(domain.getGateId());
		entity.setDirection(domain.getDirection());
		entity.setNonce(domain.getNonce());
		entity.setStatus(domain.getStatus() != null ? domain.getStatus() : AttendanceStatus.GRANTED);
		entity.setOccurredAt(domain.getOccurredAt() != null ? domain.getOccurredAt() : Instant.now());
		entity.setCreatedAt(Instant.now());
		return entity;
	}
}
