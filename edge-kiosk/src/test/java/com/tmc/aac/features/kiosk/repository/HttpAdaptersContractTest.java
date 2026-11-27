package com.tmc.aac.features.kiosk.repository;

import com.sun.net.httpserver.HttpServer;
import com.tmc.aac.features.kiosk.domain.IntegrationContracts.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class HttpAdaptersContractTest {
    HttpServer server;
    String base;
    @BeforeEach void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        base = "http://127.0.0.1:" + server.getAddress().getPort();
        server.start();
    }
    @AfterEach void stop() { server.stop(0); }
    void respond(String path, int code, String body) {
        server.createContext(path, exchange -> {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(code, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
    }
    @Test void turnstileCommandAndStatusPreserveUnknownInsteadOfReturningBooleanSuccess() {
        respond("/api/v1/commands", 200, "{\"outcome\":\"OPEN_UNKNOWN\"}");
        var adapter = new HttpTurnstileAdapter(base);
        assertThat(adapter.open(new GateCommand("T1", "G1")).outcome()).isEqualTo(GateOutcome.OPEN_UNKNOWN);
        assertThat(adapter.query("T1").outcome()).isEqualTo(GateOutcome.OPEN_UNKNOWN);
    }
    @Test void registryExplicitRejectionRemainsABusinessDecision() {
        respond("/api/v1/authorizations", 200, "{\"decision\":\"DENIED\",\"reason\":\"ANTI_PASSBACK\"}");
        assertThat(new HttpRegistryAdapter(base).query("T1").decision()).isEqualTo(Decision.DENIED);
    }
    @Test void mockUnavailableCertificateDiffersFromUncertainServerFailure() {
        respond("/not-processed", 503, "{\"notProcessed\":true}");
        respond("/uncertain", 500, "{}");
        var http = new HttpTransport(base);
        assertThatThrownBy(() -> http.post("/not-processed", "request", Boolean.class))
                .isInstanceOfSatisfying(RemoteUnavailable.class, ex -> assertThat(ex.definitelyNotSent()).isTrue());
        assertThatThrownBy(() -> http.post("/uncertain", "request", Boolean.class))
                .isInstanceOfSatisfying(RemoteUnavailable.class, ex -> assertThat(ex.definitelyNotSent()).isFalse());
    }
    @Test void coreAckUsesEventIdsWithoutAssumingWholeBatchSucceeded() {
        respond("/api/v1/events/batch", 200, "{\"acknowledgedEventIds\":[\"EV1\"]}");
        assertThat(new HttpCoreAdapter(base).send(new Batch(java.util.List.of())).acknowledgedEventIds()).containsExactly("EV1");
    }
}
