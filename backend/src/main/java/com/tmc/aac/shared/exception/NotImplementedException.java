package com.tmc.aac.shared.exception;

public class NotImplementedException extends RuntimeException {

	public NotImplementedException(String feature) {
		super("Feature not implemented yet: " + feature);
	}
}
