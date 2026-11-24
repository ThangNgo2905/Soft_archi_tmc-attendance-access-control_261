package com.tmc.aac.features.gate.domain;

import java.util.UUID;

public class GateStation {

	private UUID id;
	private String code;
	private String name;
	private String factoryCode;
	private GateDirection direction;
	private boolean active;

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
}
