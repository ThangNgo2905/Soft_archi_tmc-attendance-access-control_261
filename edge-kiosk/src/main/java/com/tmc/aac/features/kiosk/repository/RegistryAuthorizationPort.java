package com.tmc.aac.features.kiosk.repository;
import com.tmc.aac.features.kiosk.domain.IntegrationContracts.Authorization;
import com.tmc.aac.features.kiosk.domain.IntegrationContracts.AuthorizationResult;
public interface RegistryAuthorizationPort { AuthorizationResult authorize(Authorization request); }
