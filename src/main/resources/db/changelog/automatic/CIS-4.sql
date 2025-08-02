-- liquibase formatted sql

-- changeset jesus:CIS-4
CREATE TABLE file_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    filename VARCHAR(255) NOT NULL,
    path VARCHAR(512) NOT NULL,
    status VARCHAR(50),
    uploaded_at TIMESTAMP,
    processed_at TIMESTAMP,
    failed_at TIMESTAMP,
    retry_count INT,
    last_attempt_at TIMESTAMP,

    CONSTRAINT uk_file_unique_path UNIQUE (filename, path)
);