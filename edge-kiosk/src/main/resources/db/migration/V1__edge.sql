CREATE TABLE access_transaction (
    transaction_id TEXT PRIMARY KEY,
    scan_id TEXT NOT NULL UNIQUE,
    state TEXT NOT NULL,
    sequence INTEGER NOT NULL,
    registry_pending INTEGER NOT NULL DEFAULT 0,
    body TEXT NOT NULL
);
CREATE TABLE transaction_transition (
    transaction_id TEXT NOT NULL REFERENCES access_transaction(transaction_id),
    sequence INTEGER NOT NULL,
    state TEXT NOT NULL,
    reason TEXT NOT NULL,
    at TEXT NOT NULL,
    PRIMARY KEY (transaction_id, sequence)
);
CREATE TABLE outbox_event (
    event_id TEXT PRIMARY KEY,
    transaction_id TEXT NOT NULL REFERENCES access_transaction(transaction_id),
    sequence INTEGER NOT NULL,
    body TEXT NOT NULL,
    acknowledged INTEGER NOT NULL DEFAULT 0,
    attempts INTEGER NOT NULL DEFAULT 0,
    next_attempt INTEGER NOT NULL DEFAULT 0,
    UNIQUE(transaction_id, sequence)
);
CREATE INDEX outbox_due ON outbox_event(acknowledged, next_attempt);
CREATE TABLE employee_access_cache (employee_id TEXT PRIMARY KEY, body TEXT NOT NULL);
CREATE TABLE cache_metadata (id INTEGER PRIMARY KEY CHECK(id = 1), version INTEGER NOT NULL);
CREATE TABLE device_configuration (id INTEGER PRIMARY KEY CHECK(id = 1), identity TEXT NOT NULL);
INSERT INTO cache_metadata(id,version) VALUES(1, -1);
CREATE TABLE local_nonce_usage (factory_id TEXT NOT NULL, employee_id TEXT NOT NULL, nonce TEXT NOT NULL,
    transaction_id TEXT NOT NULL, PRIMARY KEY(factory_id, employee_id, nonce));
CREATE TABLE local_access_state (factory_id TEXT NOT NULL, employee_id TEXT NOT NULL,
    direction TEXT, reservation TEXT, PRIMARY KEY(factory_id,employee_id));
CREATE TABLE audit_log (id INTEGER PRIMARY KEY AUTOINCREMENT, transaction_id TEXT NOT NULL,
    actor TEXT NOT NULL, note TEXT NOT NULL, at TEXT NOT NULL);
