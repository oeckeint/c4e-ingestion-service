-- liquibase formatted sql

-- changeset jesus:CIS-4
CREATE TABLE file_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    -- Información del archivo
    filename VARCHAR(255) NOT NULL,
    origin_path VARCHAR(512) NOT NULL,
    final_path VARCHAR(512),
    extension VARCHAR(10),
    type VARCHAR(50),
    comment TEXT NULL,

    -- Estado del flujo
    status VARCHAR(50),
    origin VARCHAR(50),
    retry_count INT DEFAULT 0,
    hash VARCHAR(64),
    failure_reason VARCHAR(100) NULL,

    -- Timestamps importantes
    uploaded_at TIMESTAMP,
    processed_at TIMESTAMP,
    failed_at TIMESTAMP,
    last_attempt_at TIMESTAMP,

    -- Auditoría
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT 'system',
    updated_at TIMESTAMP,
    updated_by VARCHAR(100),

    CONSTRAINT uk_file_unique_path UNIQUE (filename, origin_path)
);
