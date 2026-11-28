package com.tmc.aac.features.kiosk.repository;
import com.tmc.aac.features.kiosk.domain.IntegrationContracts.GateResult;
public interface TurnstileStatusPort { GateResult query(String transactionId); }
