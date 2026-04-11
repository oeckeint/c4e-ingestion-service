package com.com4energy.processor.common;

import com.com4energy.i18n.core.MessageKey;

public enum IngestionCommonMessageKey implements MessageKey {

    UPLOAD_NO_FILES_PROVIDED("ingestion.upload.no.files.provided"),
    UPLOAD_BLOCKED_RECONCILIATION_RUNNING("ingestion.upload.blocked.reconciliation.running"),
    UPLOAD_EMPTY_FILES_ARRAY_LOG("ingestion.upload.empty.files.array.log"),
    UPLOAD_BATCH_RESULT("ingestion.upload.batch.result"),
    UPLOAD_BATCH_COMPLETED_LOG("ingestion.upload.batch.completed.log"),
    FILE_PENDING_REGISTERED_LOG("ingestion.file.pending.registered.log"),
    FILE_PENDING_STORAGE_FAILED_ERROR("ingestion.file.pending.storage.failed.error"),
    FILE_REJECTED_REGISTERED_LOG("ingestion.file.rejected.registered.log"),
    FILE_REJECTED_VALIDATION_LOG("ingestion.file.rejected.validation.log"),
    FILE_REJECTED_CONTENT_TYPE_DEBUG_COMMENT("ingestion.file.rejected.content-type.debug.comment"),
    FILE_REJECTED_STORAGE_ERROR_LOG("ingestion.file.rejected.storage.error.log"),
    FILE_REJECTED_MULTIPLE_REASONS_COMMENT("ingestion.file.rejected.multiple.reasons.comment"),

    // Context validation
    FAILED_FILE_CONTEXT_VALIDATION_CONTEXT_NULL("ingestion.failed.file.context.validation.context.null"),
    FAILED_FILE_CONTEXT_ALL_REASONS_NULL_OR_EMPTY("ingestion.failed.file.context.all.reasons.null.or.empty"),
    FAILED_FILE_CONTEXT_FILE_STATUS_NULL("ingestion.failed.file.context.file.status.null"),
    FILE_DUPLICATE_DETECTED_LOG("ingestion.file.duplicate.detected.log"),
    FILE_FAILED_PROCESS_LOG("ingestion.file.failed.process.log"),
    FILE_EMPTY_SKIPPED_LOG("ingestion.file.empty.skipped.log"),
    FILE_PROCESSING_IOEXCEPTION_LOG("ingestion.file.processing.ioexception.log"),
    FILE_PROCESSING_UNEXPECTED_ERROR_LOG("ingestion.file.processing.unexpected.error.log"),
    FILE_DUPLICATE_HANDLE_ERROR_LOG("ingestion.file.duplicate.handle.error.log"),
    FILE_DUPLICATE_PERSIST_ERROR_LOG("ingestion.file.duplicate.persist.error.log"),
    FILE_REJECT_EXPECTS_INVALID_CONTEXT_ERROR("ingestion.file.reject.expects-invalid-context.error"),
    FILE_SAVE_NEW_EXPECTS_INVALID_CONTEXT_ERROR("ingestion.file.save-new.expects-invalid-context.error"),
    FILE_SAVE_NEW_EXPECTS_VALID_CONTEXT_ERROR("ingestion.file.save-new.expects-valid-context.error"),
    FILE_SAVE_DUPLICATED_EXPECTS_DUPLICATED_CONTEXT_ERROR("ingestion.file.save-duplicated.expects-duplicated-context.error"),

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
     FAILURE_REASON_DUPLICATED_ORIGINAL_FILENAME("ingestion.failure.reason.duplicate-original-filename"),
     FAILURE_REASON_DUPLICATED_CONTENT("ingestion.failure.reason.duplicate-content"),
    FAILURE_REASON_UNAUTHORIZED_ACCESS("ingestion.failure.reason.unauthorized-access"),
    FAILURE_REASON_TIMEOUT("ingestion.failure.reason.timeout"),
    FAILURE_REASON_MAX_RETRIES_EXCEEDED("ingestion.failure.reason.max-retries-exceeded"),
    FAILURE_REASON_NULL_FILE("ingestion.failure.reason.null-file"),
    FAILURE_REASON_STORAGE_ERROR("ingestion.failure.reason.storage-error"),
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
