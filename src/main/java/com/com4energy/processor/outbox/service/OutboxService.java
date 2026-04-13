package com.com4energy.processor.outbox.service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import com.com4energy.processor.common.InternalServices;
import com.com4energy.processor.model.FailureReason;
import com.com4energy.processor.model.FileOrigin;
import com.com4energy.processor.model.FileRecord;
import com.com4energy.processor.model.FileStatus;
import com.com4energy.processor.outbox.domain.OutboxAggregateType;
import com.com4energy.processor.outbox.domain.OutboxEvent;
import com.com4energy.processor.outbox.domain.OutboxEventType;
import com.com4energy.processor.outbox.domain.OutboxStatus;
import com.com4energy.processor.outbox.factory.OutboxEventFactory;
import com.com4energy.processor.outbox.repository.OutboxEventRepository;
import com.com4energy.processor.service.dto.FileContext;
import com.com4energy.processor.service.dto.FileHandlingResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public FileHandlingResult saveDuplicated(FileHandlingResult currentResult, InternalServices internalService, FileOrigin origin) {
        if (!currentResult.storedInDisk() || !currentResult.fileContext().isDuplicated()) {
            return currentResult;
        }

        FileContext fileContext = currentResult.fileContext();
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("fileId", null);
        payload.put("sourceId", fileContext.validationContext().getOrComputeHash());
        payload.put("eventType", OutboxEventType.FILE_ALREADY_EXISTS.name());
        payload.put("filename", fileContext.validationContext().getOriginalFilename());
        payload.put("status", fileContext.fileStatus().name());
        payload.put("origin", origin.name());
        payload.put("reason", fileContext.getPrimaryFailureReason().name());
        payload.put("reasonDescription", fileContext.getPrimaryFailureReason().getDescription());
        payload.put("createdBy", internalService);
        payload.put("occurredAt", LocalDateTime.now().toString());

        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize duplicated file payload", e);
        }

        OutboxEvent event = OutboxEventFactory.createPending(
                OutboxAggregateType.FILE.name(),
                fileContext.validationContext().getOrComputeHash(),
                OutboxEventType.FILE_ALREADY_EXISTS.name(),
                payloadJson
        );
        outboxEventRepository.save(event);

        return currentResult.withPersistedInOutboxEvent();
    }

    @Transactional
    public FileHandlingResult saveRejected(FileHandlingResult currentResult, InternalServices internalService, FileOrigin origin) {
        if (!currentResult.storedInDisk()) {
            return currentResult;
        }

        FileContext fileContext = currentResult.fileContext();

        FailureReason safeReason = Optional.ofNullable(fileContext.getPrimaryFailureReason())
                .orElse(FailureReason.UNKNOWN_ERROR);
        LocalDateTime now = LocalDateTime.now();

        FileRecord rejectedRecord = FileRecord.builder()
                .originalFilename(fileContext.validationContext().getOriginalFilename())
                .extension(FilenameUtils.getExtension(fileContext.validationContext().getOriginalFilename()))
                .origin(origin)
                .finalPath(fileContext.findStoredFilePath().orElse(null))
                .hash(fileContext.validationContext().getOrComputeHash())
                .status(FileStatus.REJECTED)
                .failureReason(safeReason)
                .uploadedAt(now)
                .failedAt(now)
                .retryCount(0)
                .comment(safeReason.getDescription())
                .build();

        String payload = buildFileRejectedPayload(rejectedRecord, internalService);

        OutboxEvent event = OutboxEventFactory.createPending(
                OutboxAggregateType.FILE.name(),
                String.valueOf(rejectedRecord.getId()),
                OutboxEventType.FILE_REJECTED.name(),
                payload
        );
        outboxEventRepository.save(event);

        return currentResult.withPersistedInOutboxEvent();
    }

    private String buildFileRejectedPayload(FileRecord fileRecord, InternalServices internalService) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("fileId", String.valueOf(fileRecord.getId()));
        payload.put("sourceId", fileRecord.getHash());
        payload.put("eventType", OutboxEventType.FILE_REJECTED.name());
        payload.put("filename", fileRecord.getOriginalFilename());
        payload.put("extension", fileRecord.getExtension());
        payload.put("fileType", fileRecord.getType() != null ? fileRecord.getType().name() : null);
        payload.put("finalPath", fileRecord.getFinalPath());
        payload.put("status", fileRecord.getStatus().name());
        payload.put("origin", fileRecord.getOrigin() != null ? fileRecord.getOrigin().name() : null);
        payload.put("reason", fileRecord.getFailureReason().name());
        payload.put("reasonDescription", fileRecord.getFailureReason().getDescription());
        payload.put("comment", fileRecord.getComment());
        payload.put("createdBy", internalService);
        payload.put("occurredAt", LocalDateTime.now().toString());

        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize file rejected payload for fileRecordId=" + fileRecord.getId(), e);
        }
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

