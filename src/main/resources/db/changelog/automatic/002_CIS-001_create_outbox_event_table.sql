-- liquibase formatted sql

-- changeset jesus:CIS-001
-- context:dev,qa,prod
-- comment Crear tabla outbox_event con soporte para concurrencia (Outbox Pattern)

CREATE TABLE outbox_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100),

    event_type VARCHAR(100) NOT NULL,
    payload JSON NOT NULL,

    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retries INT NOT NULL DEFAULT 0,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP NULL DEFAULT NULL,

    -- control de errores
    error_message TEXT NULL,

    -- locking para concurrencia
    locked_at TIMESTAMP NULL DEFAULT NULL,
    locked_by VARCHAR(100) NULL,

    -- índices críticos para performance
    INDEX idx_status_created (status, created_at),
    INDEX idx_status_locked (status, locked_at),
    INDEX idx_aggregate (aggregate_type, aggregate_id)
);

-- rollback DROP TABLE outbox_event;