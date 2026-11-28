package com.tmc.aac.features.kiosk.repository;

import com.tmc.aac.features.kiosk.domain.IntegrationContracts.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class HttpRegistryAdapter implements RegistryAuthorizationPort, RegistryTransactionPort {
    private final HttpTransport http;
    public HttpRegistryAdapter(@Value("${edge.registry-url}") String baseUrl) { http = new HttpTransport(baseUrl); }
    @Override public AuthorizationResult authorize(Authorization request) {
        return http.post("/api/v1/authorizations", request, AuthorizationResult.class);
    }
    @Override public AuthorizationResult query(String id) {
        return http.get("/api/v1/authorizations/" + id, AuthorizationResult.class);
    }
    @Override public void outcome(RegistryOutcome outcome) {
        http.post("/api/v1/outcomes", outcome, Boolean.class);
    }
}
