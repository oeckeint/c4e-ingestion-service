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
    FILE_PROCESSING_JOB_FAILED("log.file.processing.job.failed");

    private final String key;

    LogsCommonMessageKey(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }
}
