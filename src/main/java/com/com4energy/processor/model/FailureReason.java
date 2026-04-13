package com.com4energy.processor.model;

import com.com4energy.processor.common.IngestionCommonMessageKey;
import com.com4energy.i18n.core.Messages;

public enum FailureReason {
    FILE_NOT_FOUND(IngestionCommonMessageKey.FAILURE_REASON_FILE_NOT_FOUND),
    INVALID_FILE_FORMAT(IngestionCommonMessageKey.FAILURE_REASON_INVALID_FILE_FORMAT),
    INVALID_EXTENSION(IngestionCommonMessageKey.FAILURE_REASON_INVALID_EXTENSION),
    INVALID_CONTENT_TYPE(IngestionCommonMessageKey.FAILURE_REASON_INVALID_CONTENT_TYPE),
    FILE_TOO_LARGE(IngestionCommonMessageKey.FAILURE_REASON_FILE_TOO_LARGE),
    FILE_IS_EMPTY(IngestionCommonMessageKey.FAILURE_REASON_FILE_IS_EMPTY),
    INVALID_FILENAME(IngestionCommonMessageKey.FAILURE_REASON_INVALID_FILENAME),
    PROCESSING_ERROR(IngestionCommonMessageKey.FAILURE_REASON_PROCESSING_ERROR),
    DUPLICATED_FILE(IngestionCommonMessageKey.FAILURE_REASON_DUPLICATED_FILE),
    DUPLICATED_FILENAME(IngestionCommonMessageKey.FAILURE_REASON_DUPLICATED_FILENAME),
    DUPLICATED_ORIGINAL_FILENAME(IngestionCommonMessageKey.FAILURE_REASON_DUPLICATED_ORIGINAL_FILENAME),
    DUPLICATED_CONTENT(IngestionCommonMessageKey.FAILURE_REASON_DUPLICATED_CONTENT),
    UNAUTHORIZED_ACCESS(IngestionCommonMessageKey.FAILURE_REASON_UNAUTHORIZED_ACCESS),
    TIMEOUT(IngestionCommonMessageKey.FAILURE_REASON_TIMEOUT),
    MAX_RETRIES_EXCEEDED(IngestionCommonMessageKey.FAILURE_REASON_MAX_RETRIES_EXCEEDED),
    NULL_FILE(IngestionCommonMessageKey.FAILURE_REASON_NULL_FILE),
    STORAGE_ERROR(IngestionCommonMessageKey.FAILURE_REASON_STORAGE_ERROR),
    UNKNOWN_ERROR(IngestionCommonMessageKey.FAILURE_REASON_UNKNOWN_ERROR);

    private final IngestionCommonMessageKey messageKey;

    FailureReason(IngestionCommonMessageKey messageKey) {
        this.messageKey = messageKey;
    }

    public String getDescription() {
        return Messages.get(messageKey);
    }
}
