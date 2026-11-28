package com.tmc.aac.features.kiosk.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tmc.aac.features.kiosk.domain.*;
import com.tmc.aac.features.kiosk.domain.IntegrationContracts.*;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
@DependsOn("sqliteSchema")
public class SqliteAccessStore implements AccessTransactionStore, OutboxStore, AccessCachePort {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final ObjectMapper json;
    public SqliteAccessStore(JdbcTemplate jdbc, TransactionTemplate transactions, ObjectMapper json) {
        this.jdbc = jdbc; this.transactions = transactions; this.json = json;
    }

    @Override public Optional<AccessTransaction> findByScanId(String id) {
        return one("SELECT body FROM access_transaction WHERE scan_id=?", id);
    }
    @Override public Optional<AccessTransaction> findById(String id) {
        return one("SELECT body FROM access_transaction WHERE transaction_id=?", id);
    }
    private Optional<AccessTransaction> one(String sql, String id) {
        return jdbc.query(sql, (rs, row) -> read(rs.getString(1), AccessTransaction.class), id).stream().findFirst();
    }
    @Override public void insert(AccessTransaction tx) {
        transactions.executeWithoutResult(status -> {
            jdbc.update("INSERT INTO access_transaction(transaction_id,scan_id,state,sequence,registry_pending,body) VALUES(?,?,?,?,?,?)",
                    tx.transactionId(), tx.scanId(), tx.state().name(), tx.sequence(), tx.registryPending(), write(tx));
            appendTransition(tx);
        });
    }
    @Override public void update(AccessTransaction tx) {
        transactions.executeWithoutResult(status -> {
            int count = jdbc.update("UPDATE access_transaction SET state=?,sequence=?,registry_pending=?,body=? WHERE transaction_id=? AND sequence=?",
                    tx.state().name(), tx.sequence(), tx.registryPending(), write(tx), tx.transactionId(), tx.sequence() - 1);
            if (count != 1) throw new IllegalStateException("Concurrent transaction modification");
            if (tx.state() == AccessState.GATE_COMMAND_PENDING) {
                jdbc.update("INSERT OR IGNORE INTO local_nonce_usage VALUES(?,?,?,?)", tx.factoryId(), tx.employeeId(), tx.nonce(), tx.transactionId());
                jdbc.update("INSERT INTO local_access_state(factory_id,employee_id,reservation) VALUES(?,?,?) ON CONFLICT(factory_id,employee_id) DO UPDATE SET reservation=excluded.reservation",
                        tx.factoryId(), tx.employeeId(), tx.transactionId());
            }
            appendTransition(tx);
        });
    }
    private void appendTransition(AccessTransaction tx) {
        jdbc.update("INSERT INTO transaction_transition(transaction_id,sequence,state,reason,at) VALUES(?,?,?,?,?)",
                tx.transactionId(), tx.sequence(), tx.state().name(), tx.reason(), tx.updatedAt().toString());
        if (tx.state().isOutcome()) {
            AttendanceEvent event = new AttendanceEvent(UUID.randomUUID().toString(), tx.transactionId(), tx.sequence(),
                    tx.kioskId(), tx.factoryId(), tx.gateId(), tx.employeeId(), tx.direction(), tx.state(),
                    tx.verificationMode(), tx.reason(), tx.occurredAt(), tx.updatedAt());
            jdbc.update("INSERT INTO outbox_event(event_id,transaction_id,sequence,body) VALUES(?,?,?,?)",
                    event.eventId(), event.transactionId(), event.sequence(), write(event));
        }
        // Local reservation and result must commit together, including for registry-verified scans.
        if (tx.state().isOutcome()) localOutcome(tx);
    }
    @Override public List<AccessTransaction> recent() {
        return jdbc.query("SELECT body FROM access_transaction ORDER BY rowid DESC LIMIT 100",
                (rs, row) -> read(rs.getString(1), AccessTransaction.class));
    }
    @Override public List<AccessTransaction> recoverable() {
        return jdbc.query("SELECT body FROM access_transaction WHERE state IN ('PENDING_AUTHORIZATION','GATE_COMMAND_PENDING','OPEN_UNKNOWN') ORDER BY rowid",
                (rs, row) -> read(rs.getString(1), AccessTransaction.class));
    }
    @Override public List<AccessTransaction> awaitingRegistry() {
        return jdbc.query("SELECT body FROM access_transaction WHERE registry_pending=1 ORDER BY rowid LIMIT 100",
                (rs, row) -> read(rs.getString(1), AccessTransaction.class));
    }
    @Override public void registryAcknowledged(String id) {
        jdbc.update("UPDATE access_transaction SET registry_pending=0 WHERE transaction_id=?", id);
    }
    @Override public Optional<String> reserveLocal(AccessTransaction tx) {
        return transactions.execute(status -> {
            var used = jdbc.query("SELECT transaction_id FROM local_nonce_usage WHERE factory_id=? AND employee_id=? AND nonce=?",
                    (rs, row) -> rs.getString(1), tx.factoryId(), tx.employeeId(), tx.nonce());
            if (!used.isEmpty()) return used.get(0).equals(tx.transactionId()) ? Optional.empty() : Optional.of("NONCE_ALREADY_USED");
            jdbc.update("INSERT INTO local_nonce_usage VALUES(?,?,?,?)", tx.factoryId(), tx.employeeId(), tx.nonce(), tx.transactionId());
            var state = jdbc.query("SELECT direction,reservation FROM local_access_state WHERE factory_id=? AND employee_id=?",
                    (rs, row) -> new LocalState(rs.getString(1), rs.getString(2)), tx.factoryId(), tx.employeeId());
            if (!state.isEmpty() && (state.get(0).reservation() != null || tx.direction().name().equals(state.get(0).direction())))
                return Optional.of("LOCAL_ANTI_PASSBACK");
            jdbc.update("INSERT INTO local_access_state(factory_id,employee_id,reservation) VALUES(?,?,?) ON CONFLICT(factory_id,employee_id) DO UPDATE SET reservation=excluded.reservation",
                    tx.factoryId(), tx.employeeId(), tx.transactionId());
            return Optional.empty();
        });
    }
    private record LocalState(String direction, String reservation) {}
    @Override public void localOutcome(AccessTransaction tx) {
        if (tx.employeeId().equals("UNKNOWN")) return;
        if (tx.state() == AccessState.OPEN_CONFIRMED) {
            jdbc.update("INSERT INTO local_access_state(factory_id,employee_id,direction,reservation) VALUES(?,?,?,NULL) ON CONFLICT(factory_id,employee_id) DO UPDATE SET direction=excluded.direction,reservation=NULL",
                    tx.factoryId(), tx.employeeId(), tx.direction().name());
        } else if (tx.state() == AccessState.DENIED) {
            jdbc.update("UPDATE local_access_state SET reservation=NULL WHERE factory_id=? AND employee_id=? AND reservation=?",
                    tx.factoryId(), tx.employeeId(), tx.transactionId());
        }
        // OPEN_UNKNOWN retains any local reservation.
    }
    @Override public void audit(String id, String actor, String note, Instant at) {
        jdbc.update("INSERT INTO audit_log(transaction_id,actor,note,at) VALUES(?,?,?,?)", id, actor, note, at.toString());
    }
    @Override public List<PendingEvent> due(Instant now) {
        return jdbc.query("SELECT body,attempts FROM outbox_event WHERE acknowledged=0 AND next_attempt<=? ORDER BY rowid LIMIT 100",
                (rs, row) -> new PendingEvent(read(rs.getString(1), AttendanceEvent.class), rs.getInt(2)), now.toEpochMilli());
    }
    @Override public void acknowledge(String eventId) {
        jdbc.update("UPDATE outbox_event SET acknowledged=1 WHERE event_id=?", eventId);
    }
    @Override public void retry(String eventId, Instant next) {
        jdbc.update("UPDATE outbox_event SET attempts=attempts+1,next_attempt=? WHERE event_id=? AND acknowledged=0", next.toEpochMilli(), eventId);
    }
    @Override public int pendingCount() {
        return java.util.Objects.requireNonNull(jdbc.queryForObject("SELECT COUNT(*) FROM outbox_event WHERE acknowledged=0", Integer.class));
    }
    @Override public Optional<EmployeeAccess> employee(String id) {
        return jdbc.query("SELECT body FROM employee_access_cache WHERE employee_id=?",
                (rs, row) -> read(rs.getString(1), EmployeeAccess.class), id).stream().findFirst();
    }
    @Override public long cacheVersion() {
        return java.util.Objects.requireNonNull(jdbc.queryForObject("SELECT version FROM cache_metadata WHERE id=1", Long.class));
    }
    @Override public void replaceCache(CacheSnapshot snapshot) {
        transactions.executeWithoutResult(status -> {
            if (snapshot.version() <= cacheVersion()) return;
            jdbc.update("DELETE FROM employee_access_cache");
            for (EmployeeAccess employee : snapshot.employees())
                jdbc.update("INSERT INTO employee_access_cache(employee_id,body) VALUES(?,?)", employee.employeeId(), write(employee));
            jdbc.update("UPDATE cache_metadata SET version=? WHERE id=1", snapshot.version());
        });
    }
    private String write(Object value) {
        try { return json.writeValueAsString(value); }
        catch (JsonProcessingException ex) { throw new IllegalStateException("Cannot encode local record", ex); }
    }
    private <T> T read(String value, Class<T> type) {
        try { return json.readValue(value, type); }
        catch (JsonProcessingException ex) { throw new IllegalStateException("Cannot decode local record", ex); }
    }
}
