-- init-scripts/01-init.sql
CREATE TABLE IF NOT EXISTS audit_logs
(
    id             BIGSERIAL PRIMARY KEY,
    object_id      INTEGER      NOT NULL,
    action_type    VARCHAR(50)  NOT NULL,
    event_type     VARCHAR(50)  NOT NULL,
    event_time     TIMESTAMP    NOT NULL,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    source_service VARCHAR(100) NOT NULL DEFAULT 'audit-service',
    metadata       TEXT
    );

CREATE INDEX IF NOT EXISTS idx_audit_logs_object_id ON audit_logs (object_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_action_type ON audit_logs (action_type);
CREATE INDEX IF NOT EXISTS idx_audit_logs_event_time ON audit_logs (event_time);
