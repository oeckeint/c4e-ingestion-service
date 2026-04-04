package com.com4energy.processor.outbox.service;

import java.time.LocalDateTime;
import java.util.List;

import com.com4energy.processor.outbox.domain.OutboxEvent;
import com.com4energy.processor.outbox.domain.OutboxStatus;
import com.com4energy.processor.outbox.factory.OutboxEventFactory;
import com.com4energy.processor.outbox.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;

    @Transactional
    public OutboxEvent saveRejectedFileEvent(String aggregateType, String aggregateId, String eventType, String payload) {
        OutboxEvent event = OutboxEventFactory.createPending(aggregateType, aggregateId, eventType, payload);
        return outboxEventRepository.save(event);
    }

    @Transactional
    public List<OutboxEvent> lockPendingEvents(String workerId, int batchSize) {
        List<OutboxEvent> events = outboxEventRepository.findPendingForUpdate(
                OutboxStatus.PENDING,
                PageRequest.of(0, batchSize)
        );

        LocalDateTime now = LocalDateTime.now();
        events.forEach(event -> {
            event.setStatus(OutboxStatus.PROCESSING);
            event.setLockedAt(now);
            event.setLockedBy(workerId);
        });

        return outboxEventRepository.saveAll(events);
    }

    @Transactional
    public void markProcessed(OutboxEvent event) {
        event.setStatus(OutboxStatus.PROCESSED);
        event.setProcessedAt(LocalDateTime.now());
        event.setLockedAt(null);
        event.setLockedBy(null);
        event.setErrorMessage(null);
        outboxEventRepository.save(event);
    }

    @Transactional
    public void markFailed(OutboxEvent event, String errorMessage) {
        event.setStatus(OutboxStatus.FAILED);
        event.setRetries(event.getRetries() + 1);
        event.setErrorMessage(errorMessage);
        event.setLockedAt(null);
        event.setLockedBy(null);
        outboxEventRepository.save(event);
    }
}

