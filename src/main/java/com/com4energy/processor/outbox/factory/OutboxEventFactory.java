package com.com4energy.processor.outbox.factory;

import java.time.LocalDateTime;

import com.com4energy.processor.outbox.domain.OutboxEvent;
import com.com4energy.processor.outbox.domain.OutboxStatus;

public class OutboxEventFactory {

    public static OutboxEvent createPending(String aggregateType, String aggregateId, String eventType, String payload) {
        return OutboxEvent.builder()
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .eventType(eventType)
                .payload(payload)
                .status(OutboxStatus.PENDING)
                .retries(0)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private OutboxEventFactory() {
        // Private constructor to prevent instantiation
    }
}

