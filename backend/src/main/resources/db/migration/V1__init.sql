-- Scaffold schema placeholders for TMC AAC.
-- Full domain mapping (JPA entities) comes in a later increment.

CREATE TABLE IF NOT EXISTS schema_meta (
    id          BIGSERIAL PRIMARY KEY,
    key         VARCHAR(100) NOT NULL UNIQUE,
    value       VARCHAR(255) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO schema_meta (key, value)
VALUES ('schema_version_note', 'V1 scaffold — domain tables TBD')
ON CONFLICT (key) DO NOTHING;

-- Reserved table names for upcoming features (empty stubs optional).
-- employees, gate_stations, attendance_events will be added when persistence is implemented.
