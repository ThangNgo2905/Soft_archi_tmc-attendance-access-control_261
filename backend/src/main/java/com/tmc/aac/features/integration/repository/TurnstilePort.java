package com.tmc.aac.features.integration.repository;

public interface TurnstilePort {

	boolean unlock(String gateId, String transactionId);
}
