package com.com4energy.processor.common;

import com.com4energy.i18n.core.MessageKey;

public enum LogsCommonMessageKey implements MessageKey {
    FILE_ALREADY_CLAIMED("log.file.already.claimed"),
    FILE_COULD_NOT_CLAIM("log.file.could.not.claim"),
    FILE_FOUND("log.file.found"),
    SCANNER_DIRECTORY_SCAN_ERROR("log.scanner.directory.scan.error"),
    FILE_DELETE_FAILED("log.file.delete.failed"),
    FILE_CLASSIFICATION_ERROR("log.file.classification.error"),
    FILE_CLASSIFIED("log.file.classified"),
    SCANNER_CLASSIFIED_FILE("log.scanner.classified.file"),
    COULD_NOT_DELETE_CLAIMED_FILE("log.could.not.delete.claimed.file"),
    ERROR_CLASSIFYING_CLAIMED_FILE("log.error.classifying.claimed.file"),
    FILE_PENDING_JOB_CLAIMED("log.file.processing.job.pending.claimed"),
    FILE_RETRY_JOB_CLAIMED("log.file.processing.job.retry.claimed"),
    FILE_PROCESSING_JOB_STARTED("log.file.processing.job.started"),
    FILE_PROCESSING_JOB_COMPLETED("log.file.processing.job.completed"),
    FILE_PROCESSING_JOB_DISABLED("log.file.processing.job.disabled"),
    FILE_PROCESSING_JOB_FAILED("log.file.processing.job.failed"),

    // Parser de medidas — errores de nombre de archivo
    MEASURE_FILENAME_REQUIRED("log.measure.filename.required"),
    MEASURE_FILENAME_NO_SPACES("log.measure.filename.no.spaces"),
    MEASURE_FILENAME_INVALID_LENGTH("log.measure.filename.invalid.length"),
    MEASURE_FILENAME_INVALID_SEGMENTS("log.measure.filename.invalid.segments"),
    MEASURE_FILENAME_NO_EXTENSION("log.measure.filename.no.extension"),
    MEASURE_FILENAME_INVALID_EXTENSION("log.measure.filename.invalid.extension"),
    MEASURE_FILENAME_UNKNOWN_TYPE("log.measure.filename.unknown.type"),

    // Parser de medidas — errores de línea
    MEASURE_LINE_INVALID_COLUMN_COUNT("log.measure.line.invalid.column.count"),
    MEASURE_LINE_CONVERSION_ERROR("log.measure.line.conversion.error"),
    MEASURE_FILE_KIND_UNSUPPORTED("log.measure.file.kind.unsupported"),

    // Procesamiento de medidas
    MEASURE_FILE_PROCESSED("log.measure.file.processed")

    ;

    private final String key;

    LogsCommonMessageKey(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }
}
