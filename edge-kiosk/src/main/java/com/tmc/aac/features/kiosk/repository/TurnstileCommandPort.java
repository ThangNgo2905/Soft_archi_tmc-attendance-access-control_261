package com.tmc.aac.features.kiosk.repository;
import com.tmc.aac.features.kiosk.domain.IntegrationContracts.GateCommand;
import com.tmc.aac.features.kiosk.domain.IntegrationContracts.GateResult;
public interface TurnstileCommandPort { GateResult open(GateCommand command); }
