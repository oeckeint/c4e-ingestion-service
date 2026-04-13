-- liquibase formatted sql

-- changeset jesus:CIS-004-2
CREATE TABLE measure_records (

    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_record_id BIGINT NOT NULL,
    line_number INT NOT NULL,
    kind VARCHAR(20) NOT NULL,
    cups VARCHAR(64) NOT NULL,
    measure_timestamp TIMESTAMP NOT NULL,

    tipo_medida INT NULL,
    bandera_inv_ver DECIMAL(18,6) NULL,
    actent DECIMAL(18,6) NULL,
    qactent DECIMAL(18,6) NULL,
    actsal DECIMAL(18,6) NULL,
    qactsal DECIMAL(18,6) NULL,
    rq1 DECIMAL(18,6) NULL,
    qrq1 DECIMAL(18,6) NULL,
    rq2 DECIMAL(18,6) NULL,
    qrq2 DECIMAL(18,6) NULL,
    rq3 DECIMAL(18,6) NULL,
    qrq3 DECIMAL(18,6) NULL,
    rq4 DECIMAL(18,6) NULL,
    qrq4 DECIMAL(18,6) NULL,
    medres1 DECIMAL(18,6) NULL,
    qmedres1 DECIMAL(18,6) NULL,
    medres2 DECIMAL(18,6) NULL,
    qmedres2 DECIMAL(18,6) NULL,
    metod_obt INT NULL,
    temporal INT NULL,
    indic_firmez INT NULL,
    codigo_factura VARCHAR(100) NULL,
    origen VARCHAR(255) NULL,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_measure_file_record_id (file_record_id),
    UNIQUE INDEX uk_measure_file_record_line (file_record_id, line_number),

    CONSTRAINT fk_measure_records_file_record
        FOREIGN KEY (file_record_id) REFERENCES file_records(id)
);

-- rollback DROP TABLE measure_records;

