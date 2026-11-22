package com.tmc.aac.features.attendance.domain;

import java.time.Instant;
import java.util.UUID;

public class AttendanceEvent {

	private UUID id;
	private UUID employeeId;
	private UUID gateId;
	private AttendanceDirection direction;
	private Instant occurredAt;
	private String transactionId;

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
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

	public Instant getOccurredAt() {
		return occurredAt;
	}

	public void setOccurredAt(Instant occurredAt) {
		this.occurredAt = occurredAt;
	}

	public String getTransactionId() {
		return transactionId;
	}

	public void setTransactionId(String transactionId) {
		this.transactionId = transactionId;
	}
}
