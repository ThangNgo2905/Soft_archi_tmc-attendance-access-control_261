package com.tmc.aac.features.kiosk.repository;

import com.tmc.aac.features.kiosk.domain.IntegrationContracts.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriUtils;
import java.nio.charset.StandardCharsets;

@Component
public class HttpCoreAdapter implements KioskSyncPort {
    private final HttpTransport http;
    public HttpCoreAdapter(@Value("${edge.core-url}") String baseUrl) { http = new HttpTransport(baseUrl); }
    @Override public BatchAck send(Batch batch) { return http.post("/api/v1/events/batch", batch, BatchAck.class); }
    @Override public CacheSnapshot cache(String factory) {
        return http.get("/api/v1/cache?factoryId=" + UriUtils.encodeQueryParam(factory, StandardCharsets.UTF_8), CacheSnapshot.class);
    }
    @Override public void heartbeat(Heartbeat heartbeat) { http.post("/api/v1/heartbeat", heartbeat, Boolean.class); }
}
