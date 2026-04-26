-- liquibase formatted sql

-- changeset jesus:CIS-004
CREATE TABLE file_records (

    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    original_filename VARCHAR(255) NULL COLLATE utf8mb4_bin,
    final_filename VARCHAR(255) NOT NULL COLLATE utf8mb4_bin,
    final_path VARCHAR(512),
    extension VARCHAR(10),
    type VARCHAR(50),
    comment TEXT NULL,

    -- Estado del flujo
    status VARCHAR(50),
    quality_status VARCHAR(50),
    business_result VARCHAR(50),
    origin VARCHAR(50),
    retry_count INT DEFAULT 0,
    processed_records INT,
    defected_records INT,
    parse_duration_ms BIGINT,
    processing_duration_ms BIGINT,
    hash VARCHAR(64) NOT NULL,
    failure_reason VARCHAR(100) NULL,

    -- Timestamps importantes
    uploaded_at TIMESTAMP,
    processed_at TIMESTAMP,
    failed_at TIMESTAMP,
    last_attempt_at TIMESTAMP,

    -- Distributed locking
    locked BOOLEAN DEFAULT FALSE,
    locked_by VARCHAR(100),
    locked_at TIMESTAMP,

    -- Auditoría
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT 'system',
    updated_at TIMESTAMP,
    updated_by VARCHAR(100),

    INDEX idx_original_filename (original_filename),
    INDEX idx_status_uploaded (status, uploaded_at),
    INDEX idx_claim_processing (status, locked, type, uploaded_at, id),
    INDEX idx_status_retry (status, retry_count, last_attempt_at),
    INDEX idx_locked (locked, locked_at),
    UNIQUE INDEX idx_hash (hash),
    INDEX idx_created_at (created_at)

);

-- rollback DROP TABLE file_records;