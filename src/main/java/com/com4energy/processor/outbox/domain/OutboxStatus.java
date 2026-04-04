package com.com4energy.processor.outbox.domain;

public enum OutboxStatus {
    PENDING,
    PROCESSING,
    PROCESSED,
    FAILED
}

