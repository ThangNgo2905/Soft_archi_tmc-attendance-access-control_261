package com.tmc.aac.features.kiosk;

import com.tmc.aac.features.kiosk.domain.*;
import com.tmc.aac.features.kiosk.domain.IntegrationContracts.*;
import com.tmc.aac.features.kiosk.repository.*;
import com.tmc.aac.features.kiosk.service.*;
import com.tmc.aac.shared.exception.ScanConflict;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = "edge.workers-enabled=false")
@ActiveProfiles("demo")
@AutoConfigureMockMvc
class KioskFlowTest {
    static final Path DATABASE;
    static { try { DATABASE = Files.createTempDirectory("tmc-edge-tests-").resolve("edge.db"); }
        catch (java.io.IOException ex) { throw new ExceptionInInitializerError(ex); } }
    @DynamicPropertySource static void database(DynamicPropertyRegistry properties) {
        properties.add("edge.database", DATABASE::toString);
    }
    @Autowired SqliteAccessStore store;
    @Autowired JdbcTemplate jdbc;
    @Autowired com.fasterxml.jackson.databind.ObjectMapper json;
    @Autowired org.springframework.transaction.support.TransactionTemplate transactions;
    @Autowired org.springframework.test.web.servlet.MockMvc mvc;
    final Clock clock = Clock.fixed(Instant.parse("2026-11-27T05:00:00Z"), ZoneOffset.UTC);
    final DeviceConfigurationPort device = () -> new DeviceIdentity("K1", "BD", "G1", Direction.IN, ZoneId.of("Asia/Ho_Chi_Minh"));
    RegistryAuthorizationPort authorization;
    RegistryTransactionPort registry;
    TurnstileCommandPort commands;
    TurnstileStatusPort status;
    AccessTransactionCoordinator coordinator;

    @BeforeEach void setup() {
        jdbc.update("DELETE FROM outbox_event"); jdbc.update("DELETE FROM transaction_transition");
        jdbc.update("DELETE FROM access_transaction"); jdbc.update("DELETE FROM local_access_state");
        jdbc.update("DELETE FROM local_nonce_usage"); jdbc.update("DELETE FROM audit_log");
        authorization = mock(RegistryAuthorizationPort.class); registry = mock(RegistryTransactionPort.class);
        commands = mock(TurnstileCommandPort.class); status = mock(TurnstileStatusPort.class);
        coordinator = coordinator();
        when(authorization.authorize(any())).thenReturn(new AuthorizationResult(Decision.ALLOWED, "AUTHORIZED"));
        when(commands.open(any())).thenReturn(new GateResult(GateOutcome.OPEN_CONFIRMED));
        when(registry.query(any())).thenReturn(new AuthorizationResult(Decision.ALLOWED, "AUTHORIZED"));
        when(status.query(any())).thenReturn(new GateResult(GateOutcome.OPEN_UNKNOWN));
    }
    AccessTransactionCoordinator coordinator() {
        return new AccessTransactionCoordinator(store, store, device, authorization, registry, commands, status, clock);
    }
    String token(String employeeId) {
        var employee = store.employee(employeeId).orElseThrow();
        long now = clock.instant().getEpochSecond();
        return new QrToken(employeeId, now, now + 30, UUID.randomUUID().toString(), employee.keyId(), "").sign(employee.secret()).encode();
    }
    AccessTransaction scan() { return coordinator.scan(UUID.randomUUID().toString(), token("DEMO-001")); }

    @Test void durableCommitsPrecedeBothExternalSideEffects() {
        when(authorization.authorize(any())).thenAnswer(call -> {
            Authorization request = call.getArgument(0);
            assertThat(store.findById(request.transactionId()).orElseThrow().state()).isEqualTo(AccessState.PENDING_AUTHORIZATION);
            return new AuthorizationResult(Decision.ALLOWED, "AUTHORIZED");
        });
        when(commands.open(any())).thenAnswer(call -> {
            GateCommand command = call.getArgument(0);
            assertThat(store.findById(command.transactionId()).orElseThrow().state()).isEqualTo(AccessState.GATE_COMMAND_PENDING);
            return new GateResult(GateOutcome.OPEN_CONFIRMED);
        });
        var tx = scan();
        assertThat(tx.state()).isEqualTo(AccessState.OPEN_CONFIRMED);
        assertThat(store.pendingCount()).isEqualTo(1);
        var reopened = new SqliteAccessStore(jdbc, transactions, json);
        assertThat(reopened.findById(tx.transactionId())).contains(tx);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM transaction_transition", Integer.class)).isEqualTo(3);
    }
    @Test void sameScanIdIsIdempotentAndConflictingPayloadIsRejected() {
        String payload = token("DEMO-001");
        var tx = coordinator.scan("scan-1", payload);
        assertThat(coordinator.scan("scan-1", payload)).isEqualTo(tx);
        assertThatThrownBy(() -> coordinator.scan("scan-1", payload + "x")).isInstanceOf(ScanConflict.class);
        verify(commands, times(1)).open(any());
    }
    @ParameterizedTest @ValueSource(strings = {"DEMO-002", "DEMO-003", "DEMO-OTHER"})
    void invalidEmployeePolicyNeverOpensGate(String id) {
        assertThat(coordinator.scan(UUID.randomUUID().toString(), token(id)).state()).isEqualTo(AccessState.DENIED);
        verifyNoInteractions(authorization, commands);
    }
    @Test void malformedAndBadSignatureNeverReachRegistry() {
        assertThat(coordinator.scan("bad-1", "invalid").reason()).isEqualTo("MALFORMED_QR");
        String valid = token("DEMO-001");
        assertThat(coordinator.scan("bad-2", valid.substring(0, valid.lastIndexOf('|') + 1) + "AAAA").reason()).isEqualTo("INVALID_SIGNATURE");
        verifyNoInteractions(authorization, commands);
    }
    @Test void explicitRegistryDenialNeverFallsBack() {
        when(authorization.authorize(any())).thenReturn(new AuthorizationResult(Decision.DENIED, "ANTI_PASSBACK"));
        var tx = scan();
        assertThat(tx.reason()).isEqualTo("ANTI_PASSBACK");
        assertThat(tx.verificationMode()).isEqualTo(VerificationMode.REGISTRY_VERIFIED);
        verifyNoInteractions(commands);
    }
    @Test void unreachableRegistryUsesDurableLocalReplayAndAntiPassback() {
        when(authorization.authorize(any())).thenThrow(new RemoteUnavailable(true));
        String payload = token("DEMO-001");
        var first = coordinator.scan("offline-1", payload);
        assertThat(first.state()).isEqualTo(AccessState.OPEN_CONFIRMED);
        assertThat(first.verificationMode()).isEqualTo(VerificationMode.OFFLINE_UNVERIFIED);
        assertThat(coordinator.scan("offline-2", payload).reason()).isEqualTo("NONCE_ALREADY_USED");
        assertThat(scan().reason()).isEqualTo("LOCAL_ANTI_PASSBACK");
        verify(commands, times(1)).open(any());
    }
    @Test void registryAckLostStaysPendingAndResumesSameIdAfterRestart() {
        when(authorization.authorize(any())).thenThrow(new RemoteUnavailable(false));
        var tx = scan();
        assertThat(tx.state()).isEqualTo(AccessState.PENDING_AUTHORIZATION);
        verifyNoInteractions(commands);
        coordinator().recover();
        verify(registry).query(tx.transactionId());
        verify(commands).open(new GateCommand(tx.transactionId(), "G1"));
        assertThat(store.findById(tx.transactionId()).orElseThrow().state()).isEqualTo(AccessState.OPEN_CONFIRMED);
    }
    @Test void timeoutQueriesStatusAndNeverBlindlyResendsOpen() {
        when(commands.open(any())).thenThrow(new RemoteUnavailable(false));
        var tx = scan();
        assertThat(tx.state()).isEqualTo(AccessState.OPEN_UNKNOWN);
        coordinator().recover(); coordinator().recover();
        verify(commands, times(1)).open(any());
        assertThat(scan().reason()).isEqualTo("EMPLOYEE_TRANSACTION_UNRESOLVED");
    }
    @Test void unknownOutcomeCanBeReconciledAsANewOutboxEvent() {
        when(commands.open(any())).thenReturn(new GateResult(GateOutcome.OPEN_UNKNOWN));
        var tx = scan();
        when(status.query(tx.transactionId())).thenReturn(new GateResult(GateOutcome.OPEN_CONFIRMED));
        coordinator().recover();
        assertThat(store.findById(tx.transactionId()).orElseThrow().state()).isEqualTo(AccessState.OPEN_CONFIRMED);
        var events = store.due(clock.instant());
        assertThat(events).hasSize(2);
        assertThat(events.get(0).event().eventId()).isNotEqualTo(events.get(1).event().eventId());
        assertThat(events.get(1).event().sequence()).isGreaterThan(events.get(0).event().sequence());
        verify(commands, times(1)).open(any());
    }
    @Test void crashAfterPendingCommandCommitOnlyQueriesEvenWhenNotFound() {
        when(commands.open(any())).thenThrow(new SimulatedPowerLoss());
        assertThatThrownBy(this::scan).isInstanceOf(SimulatedPowerLoss.class);
        var pending = store.recoverable().get(0);
        assertThat(pending.state()).isEqualTo(AccessState.GATE_COMMAND_PENDING);
        when(status.query(pending.transactionId())).thenReturn(new GateResult(GateOutcome.NOT_FOUND));
        coordinator().recover();
        verify(commands, times(1)).open(any());
        assertThat(store.findById(pending.transactionId()).orElseThrow().state()).isEqualTo(AccessState.OPEN_UNKNOWN);
    }
    @Test void resultAndOutboxRollbackTogetherWhenPersistenceFails() {
        jdbc.execute("CREATE TRIGGER fail_outbox BEFORE INSERT ON outbox_event BEGIN SELECT RAISE(ABORT,'injected disk failure'); END");
        try {
            assertThatThrownBy(this::scan).isInstanceOf(org.springframework.dao.DataAccessException.class);
            assertThat(store.recoverable().get(0).state()).isEqualTo(AccessState.GATE_COMMAND_PENDING);
            assertThat(store.pendingCount()).isZero();
            verify(commands, times(1)).open(any());
        } finally { jdbc.execute("DROP TRIGGER fail_outbox"); }
    }
    @Test void syncAcknowledgesOnlySubmittedIdsAndRetriesWithStableEventId() {
        scan(); coordinator.scan("invalid", "bad-qr");
        var remote = mock(KioskSyncPort.class);
        var events = store.due(clock.instant());
        when(remote.send(any())).thenReturn(new BatchAck(List.of(events.get(0).event().eventId(), "unrequested-id")));
        when(remote.cache(any())).thenReturn(new CacheSnapshot(-1, List.of()));
        var sync = new SyncAgent(store, store, remote, device, clock);
        sync.synchronize();
        assertThat(store.pendingCount()).isEqualTo(1);
        assertThat(store.due(clock.instant())).isEmpty();
        var future = Clock.offset(clock, Duration.ofSeconds(2));
        when(remote.send(any())).thenThrow(new RemoteUnavailable(false));
        new SyncAgent(store, store, remote, device, future).synchronize();
        var retained = store.due(clock.instant().plusSeconds(100));
        assertThat(retained.get(0).event().eventId()).isEqualTo(events.get(1).event().eventId());
        assertThat(retained.get(0).attempts()).isEqualTo(2);
    }
    @Test void localDatabaseFailureBeforeAuthorizationNeverOpensGate() {
        var unavailable = mock(AccessTransactionStore.class);
        when(unavailable.findByScanId(any())).thenReturn(Optional.empty());
        when(unavailable.recoverable()).thenReturn(List.of());
        doThrow(new IllegalStateException("disk full")).when(unavailable).insert(any());
        var service = new AccessTransactionCoordinator(unavailable, store, device, authorization, registry, commands, status, clock);
        assertThatThrownBy(() -> service.scan("disk", token("DEMO-001"))).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(authorization, commands);
    }
    static class SimulatedPowerLoss extends Error {}

    @Test void scannerHttpBoundaryRejectsInvalidBodiesAndNeverExposesSecrets() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/kiosk/scans")
                .contentType("application/json").content("{\"scanId\":\"valid-id\",\"payload\":null}"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/kiosk/scans")
                .contentType("application/json").content(json.writeValueAsString(java.util.Map.of("scanId", "long", "payload", "x".repeat(2049)))))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());
        var response = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/kiosk/status"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(response).doesNotContain("secret", "demo-only-not-a-production-secret");
    }
}
