package com.tmc.aac.features.attendance.repository;

import com.tmc.aac.features.attendance.domain.AttendanceDirection;
import com.tmc.aac.features.attendance.domain.AttendanceStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "attendance_events")
public class AttendanceEventJpaEntity {

	@Id
	private UUID id;

	@Column(name = "transaction_id", nullable = false, unique = true, length = 100)
	private String transactionId;

	@Column(name = "employee_id", nullable = false)
	private UUID employeeId;

	@Column(name = "gate_id", nullable = false)
	private UUID gateId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private AttendanceDirection direction;

	@Column(unique = true, length = 128)
	private String nonce;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private AttendanceStatus status;

	@Column(name = "occurred_at", nullable = false)
	private Instant occurredAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected AttendanceEventJpaEntity() {
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public String getTransactionId() {
		return transactionId;
	}

	public void setTransactionId(String transactionId) {
		this.transactionId = transactionId;
	}

	public UUID getEmployeeId() {
		return employeeId;
	}

	public void setEmployeeId(UUID employeeId) {
		this.employeeId = employeeId;
	}

	public UUID getGateId() {
		return gateId;
	}

	public void setGateId(UUID gateId) {
		this.gateId = gateId;
	}

	public AttendanceDirection getDirection() {
		return direction;
	}

	public void setDirection(AttendanceDirection direction) {
		this.direction = direction;
	}

	public String getNonce() {
		return nonce;
	}

	public void setNonce(String nonce) {
		this.nonce = nonce;
	}

	public AttendanceStatus getStatus() {
		return status;
	}

	public void setStatus(AttendanceStatus status) {
		this.status = status;
	}

	public Instant getOccurredAt() {
		return occurredAt;
	}

	public void setOccurredAt(Instant occurredAt) {
		this.occurredAt = occurredAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}
}
