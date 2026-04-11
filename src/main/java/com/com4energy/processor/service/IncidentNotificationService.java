package com.com4energy.processor.service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.UUID;

import com.com4energy.event.publisher.common.Environment;
import com.com4energy.event.publisher.core.Publisher;
import com.com4energy.event.publisher.incident.contract.IncidentCategory;
import com.com4energy.event.publisher.incident.contract.IncidentEvent;
import com.com4energy.event.publisher.incident.contract.IncidentSeverity;
import com.com4energy.event.publisher.incident.contract.IncidentStatus;
import com.com4energy.event.publisher.incident.contract.IncidentType;
import com.com4energy.processor.config.FeatureFlagService;
import com.com4energy.processor.model.FileRecord;
import com.com4energy.processor.outbox.domain.OutboxEventType;
import com.com4energy.processor.outbox.service.OutboxService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class IncidentNotificationService {

    private static final String INVALID_FILE_EVENT_TYPE = OutboxEventType.FILE_VALIDATION_INCIDENT.name();

    private final FeatureFlagService featureFlagService;
    private final ConfigurableEnvironment environment;
    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;

    private final ObjectProvider<Publisher> incidentPublisherProvider;

    @Value("${spring.application.name:c4e-ingestion-service}")
    private String serviceName;

    public void notifyProcessingError(FileRecord fileRecord, Exception exception) {
        if (!featureFlagService.isNotifyOnErrorEnabled()) {
            return;
        }

        Publisher incidentPublisher = incidentPublisherProvider.getIfAvailable();
        if (incidentPublisher == null) {
            log.debug("Incident publisher bean is not available. Skipping incident publication.");
            return;
        }

        try {
            IncidentEvent event = new IncidentEvent(
                    UUID.randomUUID().toString(),
                    serviceName,
                    resolveEnvironment(),
                    null,
                    "processFile",
                    null,
                    null,
                    null,
                    null,
                    exception.getClass().getSimpleName(),
                    exception.getMessage(),
                    stackTraceToString(exception),
                    IncidentCategory.FILE_PROCESSING,
                    IncidentSeverity.ERROR,
                    IncidentStatus.NEW,
                    "INGESTION_PROCESSING_ERROR",
                    fileRecord.getOriginalFilename(),
                    fileRecord.getExtension(),
                    fileRecord.getFinalPath(),
                    buildMetadata(fileRecord),
                    serviceName,
                    LocalDateTime.now(),
                    serviceName,
                    Instant.now()
            );

            incidentPublisher.send(IncidentType.SYSTEM, event);
        } catch (Exception publishException) {
            log.warn("Could not publish incident event for file {}", fileRecord.getOriginalFilename(), publishException);
        }
    }

    private Environment resolveEnvironment() {
        for (String profile : environment.getActiveProfiles()) {
            if ("prod".equalsIgnoreCase(profile)) {
                return Environment.PROD;
            }
            if ("qa".equalsIgnoreCase(profile)) {
                return Environment.QA;
            }
        }
        return Environment.DEV;
    }

    private String buildMetadata(FileRecord fileRecord) {
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("id", fileRecord.getId());
        metadata.put("status", fileRecord.getStatus());
        metadata.put("retryCount", fileRecord.getRetryCount());
        return toJson(metadata, "Cannot serialize incident metadata for fileRecordId=" + fileRecord.getId());
    }

    private String stackTraceToString(Exception exception) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        exception.printStackTrace(printWriter);
        return stringWriter.toString();
    }

    private String serializeIncidentPayload(IncidentEvent event) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", event.id());
        payload.put("serviceName", event.serviceName());
        payload.put("environment", event.environment());
        payload.put("endpoint", event.endpoint());
        payload.put("methodName", event.methodName());
        payload.put("httpMethod", event.httpMethod());
        payload.put("traceId", event.traceId());
        payload.put("spanId", event.spanId());
        payload.put("userId", event.userId());
        payload.put("exceptionType", event.exceptionType());
        payload.put("message", event.message());
        payload.put("stackTrace", event.stackTrace());
        payload.put("category", event.category());
        payload.put("severity", event.severity());
        payload.put("status", event.status());
        payload.put("errorCode", event.errorCode());
        payload.put("filename", event.filename());
        payload.put("fileType", event.fileType());
        payload.put("finalPath", event.finalPath());
        payload.put("metadata", event.metadata());
        payload.put("createdBy", event.createdBy());
        payload.put("updatedOn", event.updatedOn());
        payload.put("updatedBy", event.updatedBy());
        payload.put("timestamp", event.timestamp());

        return toJson(payload, "Cannot serialize incident payload for outbox. incidentId=" + event.id());
    }

    private String buildValidationMetadata(String filename, String detail) {
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("filename", filename);
        metadata.put("reason", detail);
        return toJson(metadata, "Cannot serialize incident validation metadata for originalFilename=" + filename);
    }

    private String toJson(Object value, String errorMessage) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(errorMessage, e);
        }
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return null;
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
