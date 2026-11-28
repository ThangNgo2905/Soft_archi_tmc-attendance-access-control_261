package com.tmc.aac.features.kiosk.repository;
import com.tmc.aac.features.kiosk.domain.IntegrationContracts.AuthorizationResult;
import com.tmc.aac.features.kiosk.domain.IntegrationContracts.RegistryOutcome;
public interface RegistryTransactionPort {
    AuthorizationResult query(String transactionId);
    void outcome(RegistryOutcome outcome);
}
