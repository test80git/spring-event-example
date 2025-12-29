-- Удаление таблицы если существует (для тестов)
-- DROP TABLE IF EXISTS audit_logs CASCADE;

-- Создание таблицы
CREATE TABLE IF NOT EXISTS audit_logs
(
    id             BIGSERIAL PRIMARY KEY,
    object_id      INTEGER      NOT NULL,
    action_type    VARCHAR(50)  NOT NULL,
    event_type     VARCHAR(50)  NOT NULL,
    event_time     TIMESTAMP    NOT NULL,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    source_service VARCHAR(100) NOT NULL DEFAULT 'audit-service',
    metadata       TEXT,

    -- Ограничения
    CONSTRAINT chk_action_type CHECK (action_type IN ('CREATE', 'UPDATE', 'DELETE')),
    CONSTRAINT chk_event_type CHECK (event_type IN ('BusinessEvent', 'BusinessEvent2'))
);

-- Функция для безопасного создания индексов
DO $$
    BEGIN
        -- Индекс по object_id
        IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_audit_logs_object_id') THEN
            CREATE INDEX idx_audit_logs_object_id ON audit_logs (object_id);
        END IF;

        -- Индекс по action_type
        IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_audit_logs_action_type') THEN
            CREATE INDEX idx_audit_logs_action_type ON audit_logs (action_type);
        END IF;

        -- Индекс по event_time
        IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_audit_logs_event_time') THEN
            CREATE INDEX idx_audit_logs_event_time ON audit_logs (event_time);
        END IF;

        -- Индекс по created_at
        IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_audit_logs_created_at') THEN
            CREATE INDEX idx_audit_logs_created_at ON audit_logs (created_at);
        END IF;
    END $$;
