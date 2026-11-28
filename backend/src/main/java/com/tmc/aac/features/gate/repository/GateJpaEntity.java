package com.tmc.aac.features.gate.repository;

import com.tmc.aac.features.gate.domain.GateDirection;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "gate_stations")
public class GateJpaEntity {

	@Id
	private UUID id;

	@Column(nullable = false, unique = true, length = 64)
	private String code;

	@Column(nullable = false)
	private String name;

	@Column(name = "factory_code", nullable = false, length = 64)
	private String factoryCode;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private GateDirection direction;

	@Column(nullable = false)
	private boolean active;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected GateJpaEntity() {
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getFactoryCode() {
		return factoryCode;
	}

	public void setFactoryCode(String factoryCode) {
		this.factoryCode = factoryCode;
	}

	public GateDirection getDirection() {
		return direction;
	}

	public void setDirection(GateDirection direction) {
		this.direction = direction;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}
}
