package com.tmc.aac.features.kiosk.service;

import com.tmc.aac.features.kiosk.domain.*;
import com.tmc.aac.features.kiosk.domain.IntegrationContracts.*;
import com.tmc.aac.features.kiosk.repository.*;
import com.tmc.aac.shared.exception.ScanConflict;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** A single process owns one physical lane; scheduled recovery uses the same serialization boundary. */
@Service
public class AccessTransactionCoordinator {
    private final AccessTransactionStore store;
    private final AccessCachePort cache;
    private final DeviceIdentity device;
    private final RegistryAuthorizationPort authorization;
    private final RegistryTransactionPort registry;
    private final TurnstileCommandPort commands;
    private final TurnstileStatusPort gateStatus;
    private final Clock clock;
    private final QrVerifier verifier = new QrVerifier();
    private final LocalShiftPolicy shifts = new LocalShiftPolicy();

    public AccessTransactionCoordinator(AccessTransactionStore store, AccessCachePort cache,
            DeviceConfigurationPort configuration, RegistryAuthorizationPort authorization,
            RegistryTransactionPort registry, TurnstileCommandPort commands, TurnstileStatusPort gateStatus, Clock clock) {
        this.store = store; this.cache = cache; device = configuration.device(); this.authorization = authorization;
        this.registry = registry; this.commands = commands; this.gateStatus = gateStatus; this.clock = clock;
    }

    public synchronized AccessTransaction scan(String scanId, String payload) {
        String digest = digest(payload);
        var existing = store.findByScanId(scanId);
        if (existing.isPresent()) {
            if (!existing.get().requestDigest().equals(digest)) throw new ScanConflict();
            return existing.get();
        }
        QrToken token = null;
        String denied = null;
        try {
            token = QrToken.parse(payload);
            EmployeeAccess employee = cache.employee(token.employeeId()).orElseThrow(() -> new AccessRejected("EMPLOYEE_NOT_CACHED"));
            verifier.verify(token, employee, device, clock.instant());
            shifts.verify(employee, clock.instant(), device.zone());
            String employeeId = token.employeeId();
            if (store.recoverable().stream().anyMatch(tx -> tx.employeeId().equals(employeeId)))
                throw new AccessRejected("EMPLOYEE_TRANSACTION_UNRESOLVED");
        } catch (AccessRejected ex) { denied = ex.getMessage(); }
        var now = clock.instant();
        AccessTransaction tx = new AccessTransaction(UUID.randomUUID().toString(), scanId, digest,
                token == null ? "UNKNOWN" : token.employeeId(), token == null ? "" : token.nonce(),
                device.kioskId(), device.factoryId(), device.gateId(), device.direction(),
                AccessState.PENDING_AUTHORIZATION, VerificationMode.NOT_VERIFIED, "PENDING", now, now, 0, false);
        store.insert(tx);
        if (denied != null) return change(tx, AccessState.DENIED, VerificationMode.NOT_VERIFIED, denied, false);
        return authorize(tx, true);
    }

    private AccessTransaction authorize(AccessTransaction tx, boolean allowFallback) {
        AuthorizationResult result;
        try {
            // Retry after restart queries the same transaction before reauthorizing.
            result = allowFallback ? authorization.authorize(request(tx)) : registry.query(tx.transactionId());
            if (result.decision() == Decision.NOT_FOUND) result = authorization.authorize(request(tx));
        } catch (RemoteUnavailable ex) {
            // After a lost ACK or restart, fallback could bypass an existing reservation.
            if (!allowFallback || !ex.definitelyNotSent()) return tx;
            var rejection = store.reserveLocal(tx);
            if (rejection.isPresent()) return change(tx, AccessState.DENIED, VerificationMode.OFFLINE_UNVERIFIED, rejection.get(), false);
            return open(tx, VerificationMode.OFFLINE_UNVERIFIED);
        }
        if (result.decision() == Decision.DENIED)
            return change(tx, AccessState.DENIED, VerificationMode.REGISTRY_VERIFIED, result.reason(), false);
        if (result.decision() != Decision.ALLOWED) return tx;
        return open(tx, VerificationMode.REGISTRY_VERIFIED);
    }

    private AccessTransaction open(AccessTransaction tx, VerificationMode mode) {
        AccessTransaction pending = change(tx, AccessState.GATE_COMMAND_PENDING, mode, "AUTHORIZED", false);
        try { return finish(pending, commands.open(new GateCommand(tx.transactionId(), tx.gateId())).outcome()); }
        catch (RemoteUnavailable ex) { return queryGate(pending); }
    }

    private AccessTransaction queryGate(AccessTransaction tx) {
        try { return finish(tx, gateStatus.query(tx.transactionId()).outcome()); }
        catch (RemoteUnavailable ex) { return finish(tx, GateOutcome.OPEN_UNKNOWN); }
    }

    private AccessTransaction finish(AccessTransaction tx, GateOutcome outcome) {
        AccessState state = switch (outcome) {
            case OPEN_CONFIRMED -> AccessState.OPEN_CONFIRMED;
            case DENIED -> AccessState.DENIED;
            case OPEN_UNKNOWN, NOT_FOUND -> AccessState.OPEN_UNKNOWN;
        };
        if (tx.state() == state) return tx;
        return change(tx, state, tx.verificationMode(), state.name(), tx.verificationMode() == VerificationMode.REGISTRY_VERIFIED);
    }

    private AccessTransaction change(AccessTransaction tx, AccessState state, VerificationMode mode, String reason, boolean notifyRegistry) {
        AccessTransaction changed = tx.transition(state, mode, reason, clock.instant(), notifyRegistry);
        store.update(changed);
        return changed;
    }

    public synchronized void recover() {
        for (AccessTransaction tx : store.recoverable()) {
            if (tx.state() == AccessState.PENDING_AUTHORIZATION) authorize(tx, false);
            else queryGate(tx); // Never sends a gate command, even if the status query says NOT_FOUND.
        }
        for (AccessTransaction tx : store.awaitingRegistry()) {
            try {
                registry.outcome(new RegistryOutcome(tx.transactionId(), tx.state()));
                store.registryAcknowledged(tx.transactionId());
            } catch (RemoteUnavailable ex) { /* Durable registry_pending will retry next tick. */ }
        }
    }

    private Authorization request(AccessTransaction tx) {
        return new Authorization(tx.transactionId(), tx.factoryId(), tx.employeeId(), tx.nonce(), tx.direction());
    }
    private String digest(String payload) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(payload.getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
}
