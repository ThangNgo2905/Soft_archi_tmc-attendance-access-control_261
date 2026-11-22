package com.tmc.aac.shared.exception;

public class ResourceNotFoundException extends RuntimeException {

	public ResourceNotFoundException(String resource, Object id) {
		super(resource + " not found: " + id);
	}
}
