package com.tmc.aac.features.kiosk.repository;

import com.tmc.aac.features.kiosk.domain.IntegrationContracts.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class HttpTurnstileAdapter implements TurnstileCommandPort, TurnstileStatusPort {
    private final HttpTransport http;
    public HttpTurnstileAdapter(@Value("${edge.turnstile-url}") String baseUrl) { http = new HttpTransport(baseUrl); }
    @Override public GateResult open(GateCommand command) { return http.post("/api/v1/commands", command, GateResult.class); }
    @Override public GateResult query(String id) { return http.get("/api/v1/commands/" + id, GateResult.class); }
}
