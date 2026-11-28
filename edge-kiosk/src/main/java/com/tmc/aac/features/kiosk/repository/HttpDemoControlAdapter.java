package com.tmc.aac.features.kiosk.repository;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("demo")
@Component
public class HttpDemoControlAdapter implements DemoControlPort {
    private record ModeRequest(String mode) {}
    private record ResolveRequest(String transactionId, String outcome) {}
    private final Map<String, HttpTransport> transports;
    public HttpDemoControlAdapter(@Value("${edge.registry-url}") String registry,
            @Value("${edge.turnstile-url}") String turnstile, @Value("${edge.core-url}") String core) {
        transports = Map.of("registry", new HttpTransport(registry), "turnstile", new HttpTransport(turnstile), "core", new HttpTransport(core));
    }
    private HttpTransport client(String service) {
        var client = transports.get(service);
        if (client == null) throw new IllegalArgumentException("Unknown mock");
        return client;
    }
    @Override public MockState state(String service) { return client(service).get("/demo/state", MockState.class); }
    @Override public MockState mode(String service, String mode) { return client(service).post("/demo/mode", new ModeRequest(mode), MockState.class); }
    @Override public void resolve(String id, String outcome) { client("turnstile").post("/demo/resolve", new ResolveRequest(id, outcome), Boolean.class); }
}
