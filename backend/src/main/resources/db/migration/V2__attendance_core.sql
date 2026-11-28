-- Core tables for TMC Attendance & Access Control (Milestone 1)

CREATE TABLE IF NOT EXISTS employees (
    id            UUID PRIMARY KEY,
    employee_code VARCHAR(64)  NOT NULL UNIQUE,
    full_name     VARCHAR(255) NOT NULL,
    factory_code  VARCHAR(64)  NOT NULL,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS gate_stations (
    id           UUID PRIMARY KEY,
    code         VARCHAR(64)  NOT NULL UNIQUE,
    name         VARCHAR(255) NOT NULL,
    factory_code VARCHAR(64)  NOT NULL,
    direction    VARCHAR(10)  NOT NULL CHECK (direction IN ('IN', 'OUT')),
    active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS attendance_events (
    id             UUID PRIMARY KEY,
    transaction_id VARCHAR(100) NOT NULL UNIQUE,
    employee_id    UUID         NOT NULL REFERENCES employees (id),
    gate_id        UUID         NOT NULL REFERENCES gate_stations (id),
    direction      VARCHAR(10)  NOT NULL CHECK (direction IN ('IN', 'OUT')),
    nonce          VARCHAR(128) UNIQUE,
    status         VARCHAR(32)  NOT NULL DEFAULT 'GRANTED',
    occurred_at    TIMESTAMPTZ  NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_attendance_events_employee_occurred
    ON attendance_events (employee_id, occurred_at DESC);

CREATE INDEX IF NOT EXISTS idx_attendance_events_occurred
    ON attendance_events (occurred_at DESC);

-- Append-only enforcement for attendance audit integrity (M1 ADR)
CREATE OR REPLACE FUNCTION prevent_attendance_event_mutation()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'attendance_events is append-only; % is not allowed', TG_OP;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_attendance_events_append_only ON attendance_events;
CREATE TRIGGER trg_attendance_events_append_only
    BEFORE UPDATE OR DELETE ON attendance_events
    FOR EACH ROW
    EXECUTE FUNCTION prevent_attendance_event_mutation();

-- Seed baseline factories, gate stations, and employees for immediate local/Compose verification
INSERT INTO employees (id, employee_code, full_name, factory_code, active)
VALUES
    ('11111111-1111-1111-1111-111111111001', 'EMP001', 'Nguyen Van An', 'Binh Duong', TRUE),
    ('11111111-1111-1111-1111-111111111002', 'EMP002', 'Tran Thi Binh', 'Binh Duong', TRUE),
    ('11111111-1111-1111-1111-111111111003', 'EMP003', 'Le Van Cuong', 'Long An', TRUE)
ON CONFLICT (employee_code) DO NOTHING;

INSERT INTO gate_stations (id, code, name, factory_code, direction, active)
VALUES
    ('22222222-2222-2222-2222-222222222001', 'GATE_BD_IN_01', 'Cong Vao 01 - Binh Duong', 'Binh Duong', 'IN', TRUE),
    ('22222222-2222-2222-2222-222222222002', 'GATE_BD_OUT_01', 'Cong Ra 01 - Binh Duong', 'Binh Duong', 'OUT', TRUE),
    ('22222222-2222-2222-2222-222222222003', 'GATE_LA_IN_01', 'Cong Vao 01 - Long An', 'Long An', 'IN', TRUE),
    ('22222222-2222-2222-2222-222222222004', 'GATE_LA_OUT_01', 'Cong Ra 01 - Long An', 'Long An', 'OUT', TRUE)
ON CONFLICT (code) DO NOTHING;
