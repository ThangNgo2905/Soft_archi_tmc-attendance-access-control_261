package com.tmc.aac.features.kiosk.repository;
import com.tmc.aac.features.kiosk.domain.AccessTransaction;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
public interface AccessTransactionStore {
    Optional<AccessTransaction> findByScanId(String scanId);
    Optional<AccessTransaction> findById(String id);
    void insert(AccessTransaction transaction);
    void update(AccessTransaction transaction);
    List<AccessTransaction> recent();
    List<AccessTransaction> recoverable();
    List<AccessTransaction> awaitingRegistry();
    void registryAcknowledged(String id);
    /** Atomically consumes nonce and reserves the employee; returns a denial reason or empty on success. */
    Optional<String> reserveLocal(AccessTransaction transaction);
    void localOutcome(AccessTransaction transaction);
    void audit(String id, String actor, String note, Instant at);
}
