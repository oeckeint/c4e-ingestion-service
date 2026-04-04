package com.com4energy.processor.common;

import com.com4energy.i18n.core.MessageKey;

public enum IngestionCommonMessageKey implements MessageKey {

    UPLOAD_NO_FILES_PROVIDED("ingestion.upload.no.files.provided"),
    UPLOAD_EMPTY_FILES_ARRAY_LOG("ingestion.upload.empty.files.array.log"),
    UPLOAD_BATCH_RESULT("ingestion.upload.batch.result"),
    UPLOAD_BATCH_COMPLETED_LOG("ingestion.upload.batch.completed.log"),
    FILE_PENDING_REGISTERED_LOG("ingestion.file.pending.registered.log"),
    FILE_REJECTED_REGISTERED_LOG("ingestion.file.rejected.registered.log"),
    FILE_REJECTED_VALIDATION_LOG("ingestion.file.rejected.validation.log"),

    FAILURE_REASON_FILE_NOT_FOUND("ingestion.failure.reason.file-not-found"),
    FAILURE_REASON_INVALID_FILE_FORMAT("ingestion.failure.reason.invalid-file-format"),
    FAILURE_REASON_INVALID_EXTENSION("ingestion.failure.reason.invalid-extension"),
    FAILURE_REASON_INVALID_CONTENT_TYPE("ingestion.failure.reason.invalid-content-type"),
    FAILURE_REASON_FILE_TOO_LARGE("ingestion.failure.reason.file-too-large"),
    FAILURE_REASON_FILE_IS_EMPTY("ingestion.failure.reason.file-is-empty"),
    FAILURE_REASON_INVALID_FILENAME("ingestion.failure.reason.invalid-filename"),
    FAILURE_REASON_PROCESSING_ERROR("ingestion.failure.reason.processing-error"),
    FAILURE_REASON_DUPLICATED_FILE("ingestion.failure.reason.duplicate-file"),
    FAILURE_REASON_DUPLICATED_FILENAME("ingestion.failure.reason.duplicate-filename"),
    FAILURE_REASON_UNAUTHORIZED_ACCESS("ingestion.failure.reason.unauthorized-access"),
    FAILURE_REASON_TIMEOUT("ingestion.failure.reason.timeout"),
    FAILURE_REASON_MAX_RETRIES_EXCEEDED("ingestion.failure.reason.max-retries-exceeded"),
    FAILURE_REASON_NULL_FILE("ingestion.failure.reason.null-file"),
    FAILURE_REASON_UNKNOWN_ERROR("ingestion.failure.reason.unknown-error")
    ;

    private final String key;

    IngestionCommonMessageKey(String key) {
        this.key = key;
    }

    @Override
    public String key() {
        return key;
    }

}
