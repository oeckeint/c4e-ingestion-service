package com.com4energy.processor.model;

public enum FailureReason {
    FILE_NOT_FOUND,
    INVALID_FILE_FORMAT,
    PROCESSING_ERROR,
    DUPLICATE_FILE,
    UNAUTHORIZED_ACCESS,
    TIMEOUT,
    MAX_RETRIES_EXCEEDED,
    UNKNOWN_ERROR
}
