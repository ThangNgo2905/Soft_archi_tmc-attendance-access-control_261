package com.tmc.aac.features.kiosk.service;

import com.tmc.aac.features.kiosk.repository.AccessTransactionStore;
import com.tmc.aac.features.kiosk.repository.DemoControlPort;
import com.tmc.aac.features.kiosk.repository.RemoteUnavailable;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("demo")
public class DemoControlService {
    public record MockView(String service, String mode, int openCount, int eventCount, int authorizationCount) {}
    private final DemoControlPort control;
    private final AccessTransactionStore store;
    private final AccessTransactionCoordinator coordinator;
    private final Clock clock;
    public DemoControlService(DemoControlPort control, AccessTransactionStore store,
            AccessTransactionCoordinator coordinator, Clock clock) {
        this.control = control; this.store = store; this.coordinator = coordinator; this.clock = clock;
    }
    public List<MockView> states() {
        return List.of("registry", "turnstile", "core").stream().map(service -> {
            try { return view(control.state(service)); }
            catch (RemoteUnavailable ex) { return new MockView(service, "UNREACHABLE", 0, 0, 0); }
        }).toList();
    }
    public MockView mode(String service, String mode) { return view(control.mode(service, mode)); }
    public void resolve(String id, String outcome) {
        UUID.fromString(id);
        if (!List.of("OPEN_CONFIRMED", "DENIED").contains(outcome)) throw new IllegalArgumentException("Invalid outcome");
        store.audit(id, "DEMO_OPERATOR", "Simulated sensor resolution requested: " + outcome, clock.instant());
        control.resolve(id, outcome);
        coordinator.recover();
    }
    private MockView view(DemoControlPort.MockState state) {
        return new MockView(state.service(), state.mode(), state.openCount(), state.eventCount(), state.authorizationCount());
    }
}
