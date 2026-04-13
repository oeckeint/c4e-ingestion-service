package com.com4energy.processor.service.impl;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import com.com4energy.processor.config.properties.FileUploadProperties;
import com.com4energy.processor.config.properties.features.ProcessorFeatures;
import lombok.NonNull;
import org.springframework.stereotype.Service;
import com.com4energy.processor.config.InstanceIdentifier;
import com.com4energy.processor.model.FailureReason;
import com.com4energy.processor.model.FileRecord;
import com.com4energy.processor.model.FileStatus;
import com.com4energy.processor.model.QualityStatus;
import com.com4energy.processor.outbox.factory.OutboxEventFactory;
import com.com4energy.processor.outbox.repository.OutboxEventRepository;
import com.com4energy.processor.service.FileProcessingService;
import com.com4energy.processor.service.FileRecordService;
import com.com4energy.processor.service.IncidentNotificationService;
import com.com4energy.processor.service.processing.FileTypeProcessingResult;
import com.com4energy.processor.service.processing.FileTypeProcessor;
import com.com4energy.processor.service.processing.FileTypeProcessorRegistry;
import com.com4energy.processor.util.FileStorageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileProcessingServiceImpl implements FileProcessingService {

    private static final Set<FileStatus> CLAIMABLE_STATUSES = Set.of(FileStatus.PENDING, FileStatus.RETRY);

    private final FileRecordService fileRecordService;
    private final FileStorageUtil fileStorageUtil;
    private final IncidentNotificationService incidentNotificationService;
    private final InstanceIdentifier instanceIdentifier;
    private final FileTypeProcessorRegistry fileTypeProcessorRegistry;
    private final ProcessorFeatures processorFeatures;
    private final FileUploadProperties fileUploadProperties;
    private final OutboxEventRepository outboxEventRepository;

    @Override
    public void processFile(@NonNull FileRecord fileRecord) {
        if (fileTypeProcessorRegistry.findProcessor(fileRecord.getType()).isEmpty()) {
            log.info("Skipping file '{}' because type '{}' is not yet processable", fileRecord.getOriginalFilename(), fileRecord.getType());
            return;
        }

        String instanceId = instanceIdentifier.getInstanceId();
        FileRecord claimedRecord = ensureClaimedByCurrentInstance(fileRecord, instanceId);
        if (claimedRecord == null) {
            log.info("Skipping file '{}' because it could not be claimed for processing", fileRecord.getOriginalFilename());
            return;
        }

        long startedAtNanos = System.nanoTime();
        Path processingPath = null;

        try {
            processingPath = moveToProcessing(claimedRecord);
            claimedRecord.setFinalPath(processingPath.toAbsolutePath().toString());
            claimedRecord.markAsProcessing();
            saveOwnedOrThrow(claimedRecord, instanceId, startedAtNanos);

            FileTypeProcessor processor = fileTypeProcessorRegistry.getRequiredProcessor(claimedRecord.getType());
            FileTypeProcessingResult result = processor.process(claimedRecord, processingPath);
            applyProcessingResult(claimedRecord, processingPath, result, instanceId, startedAtNanos);
            publishDeferredOutboxEvents(claimedRecord, result);

            log.debug("File '{}' finished processing with outcome '{}'", claimedRecord.getOriginalFilename(), result.status());
        } catch (LockOwnershipException e) {
            log.warn("Ownership lost while processing file '{}': {}", claimedRecord.getOriginalFilename(), e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error while processing file '{}'", claimedRecord.getOriginalFilename(), e);
            handleUnexpectedProcessingErrorSafely(claimedRecord, processingPath, e, instanceId, startedAtNanos);
            incidentNotificationService.notifyProcessingError(claimedRecord, e);
        } finally {
            releaseLock(claimedRecord, instanceId);
        }
    }

    private FileRecord ensureClaimedByCurrentInstance(FileRecord fileRecord, String instanceId) {
        if (Boolean.TRUE.equals(fileRecord.getLocked()) && instanceId.equals(fileRecord.getLockedBy())) {
            return fileRecord;
        }

        return fileRecordService.claimFileForProcessing(
                        fileRecord.getId(),
                        CLAIMABLE_STATUSES,
                        fileTypeProcessorRegistry.supportedTypes(),
                        instanceId
                )
                .orElse(null);
    }

    private Path moveToProcessing(FileRecord fileRecord) {
        Path currentPath = Paths.get(fileRecord.getFinalPath()).toAbsolutePath().normalize();
        if (!java.nio.file.Files.exists(currentPath)) {
            throw new IllegalStateException("Could not locate file for processing: " + currentPath);
        }

        Path processingRoot = Paths.get(fileUploadProperties.processingPath()).toAbsolutePath().normalize();
        if (currentPath.startsWith(processingRoot)) {
            return currentPath;
        }

        Path movedPath = fileStorageUtil.moveFileToProcessing(currentPath.toFile());
        if (movedPath == null) {
            throw new IllegalStateException("Could not move file to processing path: " + currentPath);
        }
        return movedPath.toAbsolutePath().normalize();
    }

    private void applyProcessingResult(
            FileRecord fileRecord,
            Path processingPath,
            FileTypeProcessingResult result,
            String instanceId,
            long startedAtNanos
    ) {
        if (result.resolvedType() != null && result.resolvedType() != fileRecord.getType()) {
            fileRecord.setType(result.resolvedType());
        }

        switch (result.status()) {
            case SUCCEEDED -> markAsSucceeded(fileRecord, processingPath, instanceId, startedAtNanos);
            case FAILED -> markAsFailed(fileRecord, processingPath, result.failureReason(), result.comment(), instanceId, startedAtNanos);
            case REJECTED -> markAsRejected(fileRecord, processingPath, result.failureReason(), result.comment(), instanceId, startedAtNanos);
        }
    }

    private void markAsSucceeded(FileRecord fileRecord, Path processingPath, String instanceId, long startedAtNanos) {
        Path processedPath = fileStorageUtil.moveFileFromProcessingToProcessed(processingPath.toFile());
        if (processedPath == null) {
            throw new IllegalStateException("Could not move file to processed path: " + processingPath);
        }

        fileRecord.setFinalPath(processedPath.toAbsolutePath().toString());
        fileRecord.setFailureReason(null);
        if (fileRecord.getQualityStatus() != QualityStatus.WITH_DEFECTS) {
            fileRecord.setComment(null);
        }
        fileRecord.markAsSucceded();
        saveOwnedOrThrow(fileRecord, instanceId, startedAtNanos);
    }

    private void markAsFailed(
            FileRecord fileRecord,
            Path processingPath,
            FailureReason failureReason,
            String comment,
            String instanceId,
            long startedAtNanos
    ) {
        Path failedPath = fileStorageUtil.moveFileToFailed(processingPath.toFile());
        if (failedPath == null) {
            throw new IllegalStateException("Could not move file to failed path: " + processingPath);
        }

        fileRecord.setFinalPath(failedPath.toAbsolutePath().toString());
        fileRecord.setFailureReason(failureReason != null ? failureReason : FailureReason.PROCESSING_ERROR);
        fileRecord.markAsFailedWithComment(comment);
        saveOwnedOrThrow(fileRecord, instanceId, startedAtNanos);
    }

    private void markAsRejected(
            FileRecord fileRecord,
            Path processingPath,
            FailureReason failureReason,
            String comment,
            String instanceId,
            long startedAtNanos
    ) {
        Path rejectedPath = fileStorageUtil.moveFileToRejected(processingPath.toFile());
        if (rejectedPath == null) {
            throw new IllegalStateException("Could not move file to rejected path: " + processingPath);
        }

        fileRecord.setFinalPath(rejectedPath.toAbsolutePath().toString());
        fileRecord.setFailureReason(failureReason != null ? failureReason : FailureReason.PROCESSING_ERROR);
        fileRecord.setComment(comment);
        fileRecord.markAsRejected();
        saveOwnedOrThrow(fileRecord, instanceId, startedAtNanos);
    }

    private void handleUnexpectedProcessingError(
            FileRecord fileRecord,
            Path processingPath,
            Exception error,
            String instanceId,
            long startedAtNanos
    ) {
        if (shouldRetry(fileRecord)) {
            Path retryPath = moveToPendingForRetry(fileRecord, processingPath);
            fileRecord.setFinalPath(retryPath.toAbsolutePath().toString());
            fileRecord.setComment(error.getMessage());
            fileRecord.markAsRetry(FailureReason.PROCESSING_ERROR);
            saveOwnedOrThrow(fileRecord, instanceId, startedAtNanos);
            return;
        }

        Path failedPath = moveToFailedAfterUnexpectedError(fileRecord, processingPath);
        fileRecord.setFinalPath(failedPath.toAbsolutePath().toString());
        fileRecord.setFailureReason(FailureReason.MAX_RETRIES_EXCEEDED);
        fileRecord.markAsFailedWithComment("Max retries exceeded after processing error: " + error.getMessage());
        saveOwnedOrThrow(fileRecord, instanceId, startedAtNanos);
    }

    private void handleUnexpectedProcessingErrorSafely(
            FileRecord fileRecord,
            Path processingPath,
            Exception error,
            String instanceId,
            long startedAtNanos
    ) {
        try {
            handleUnexpectedProcessingError(fileRecord, processingPath, error, instanceId, startedAtNanos);
        } catch (Exception remediationError) {
            log.error("Could not complete remediation for file '{}' after processing error", fileRecord.getOriginalFilename(), remediationError);
            fileRecord.setFailureReason(FailureReason.PROCESSING_ERROR);
            fileRecord.markAsFailedWithComment(
                    "Processing remediation failed after error '" + error.getMessage() + "': " + remediationError.getMessage()
            );
            fileRecord.setProcessingDurationMs(elapsedMillis(startedAtNanos));
            boolean persisted = fileRecordService.saveIfOwnedBy(fileRecord, instanceId);
            if (!persisted) {
                log.warn("Could not persist remediation fallback due to lock ownership mismatch. fileId={}, instanceId={}",
                        fileRecord.getId(), instanceId);
            }
        }
    }

    private boolean shouldRetry(FileRecord fileRecord) {
        int currentRetries = fileRecord.getRetryCount() == null ? 0 : fileRecord.getRetryCount();
        return currentRetries < processorFeatures.getMaxRetries();
    }

    private Path moveToPendingForRetry(FileRecord fileRecord, Path processingPath) {
        Path sourcePath = processingPath != null ? processingPath : Paths.get(fileRecord.getFinalPath()).toAbsolutePath().normalize();
        Path pendingPath = fileStorageUtil.moveFileToPending(sourcePath.toFile());
        if (pendingPath == null) {
            throw new IllegalStateException("Could not move file back to pending path: " + sourcePath);
        }
        return pendingPath;
    }

    private Path moveToFailedAfterUnexpectedError(FileRecord fileRecord, Path processingPath) {
        Path sourcePath = processingPath != null ? processingPath : Paths.get(fileRecord.getFinalPath()).toAbsolutePath().normalize();
        Path failedPath = fileStorageUtil.moveFileToFailed(sourcePath.toFile());
        if (failedPath == null) {
            throw new IllegalStateException("Could not move file to failed path after unexpected error: " + sourcePath);
        }
        return failedPath;
    }

    private void releaseLock(FileRecord fileRecord, String instanceId) {
        boolean released = fileRecordService.releaseLockIfOwnedBy(fileRecord, instanceId);
        if (!released) {
            log.warn("Could not release lock for file '{}' owned by '{}'", fileRecord.getOriginalFilename(), instanceId);
        }
    }

    private void saveOwnedOrThrow(FileRecord fileRecord, String instanceId, long startedAtNanos) {
        fileRecord.setProcessingDurationMs(elapsedMillis(startedAtNanos));
        boolean saved = fileRecordService.saveIfOwnedBy(fileRecord, instanceId);
        if (!saved) {
            throw new LockOwnershipException(fileRecord.getId(), instanceId);
        }
    }

    private void publishDeferredOutboxEvents(FileRecord fileRecord, FileTypeProcessingResult result) {
        for (FileTypeProcessingResult.DeferredOutboxEvent deferredEvent : result.deferredOutboxEvents()) {
            try {
                String payload = switch (deferredEvent.eventType()) {
                    case FILE_DEFECT_REPORT_CREATED -> buildDefectReportCreatedPayload(fileRecord, deferredEvent);
                    case FILE_PERSISTENCE_QUARANTINE -> buildPersistenceQuarantinePayload(fileRecord, deferredEvent);
                    default -> throw new IllegalArgumentException("Unsupported deferred outbox event: " + deferredEvent.eventType());
                };

                outboxEventRepository.save(
                        OutboxEventFactory.createPending(
                                "FileRecord",
                                String.valueOf(fileRecord.getId()),
                                deferredEvent.eventType().name(),
                                payload
                        )
                );
            } catch (Exception ex) {
                log.warn(
                        "Could not publish deferred outbox event {} for fileId={}: {}",
                        deferredEvent.eventType(),
                        fileRecord.getId(),
                        ex.getMessage(),
                        ex
                );
            }
        }
    }

    private String buildDefectReportCreatedPayload(
            FileRecord fileRecord,
            FileTypeProcessingResult.DeferredOutboxEvent deferredEvent
    ) {
        String safeOrigin = safeName(fileRecord.getOrigin());
        String safeType = safeName(fileRecord.getType());
        String safeStatus = safeName(fileRecord.getStatus());
        String safeComment = escapeJson(fileRecord.getComment());
        String finalPath = fileRecord.getFinalPath() != null
                ? "\"" + escapeJson(fileRecord.getFinalPath()) + "\""
                : "null";

        return "{"
                + "\"fileRecordId\":" + fileRecord.getId() + ","
                + "\"sourceId\":\"" + fileRecord.getId() + "\","
                + "\"eventType\":\"" + deferredEvent.eventType().name() + "\","
                + "\"filename\":\"" + escapeJson(fileRecord.getOriginalFilename()) + "\","
                + "\"fileType\":\"" + safeType + "\","
                + "\"status\":\"" + safeStatus + "\","
                + "\"origin\":\"" + safeOrigin + "\","
                + "\"finalPath\":" + finalPath + ","
                + "\"comment\":\"" + safeComment + "\","
                + "\"lineReference\":\"" + escapeJson(deferredEvent.reportPath().toAbsolutePath().toString()) + "\","
                + "\"occurredAt\":\"" + java.time.LocalDateTime.now() + "\","
                + "\"metadata\":{"
                + "\"phase\":\"" + escapeJson(deferredEvent.phase()) + "\","
                + "\"incidentCount\":" + deferredEvent.incidentCount() + ","
                + "\"defectFilePath\":\"" + escapeJson(deferredEvent.reportPath().toAbsolutePath().toString()) + "\""
                + "}"
                + "}";
    }

    private String buildPersistenceQuarantinePayload(
            FileRecord fileRecord,
            FileTypeProcessingResult.DeferredOutboxEvent deferredEvent
    ) {
        String safeOrigin = safeName(fileRecord.getOrigin());
        String safeType = safeName(fileRecord.getType());
        String safeStatus = safeName(fileRecord.getStatus());
        String finalPath = fileRecord.getFinalPath() != null
                ? "\"" + escapeJson(fileRecord.getFinalPath()) + "\""
                : "null";
        String quarantinePath = deferredEvent.reportPath() != null
                ? "\"" + escapeJson(deferredEvent.reportPath().toAbsolutePath().toString()) + "\""
                : "null";

        return "{"
                + "\"fileRecordId\":" + fileRecord.getId() + ","
                + "\"sourceId\":\"" + fileRecord.getId() + "\","
                + "\"eventType\":\"" + deferredEvent.eventType().name() + "\","
                + "\"filename\":\"" + escapeJson(fileRecord.getOriginalFilename()) + "\","
                + "\"fileType\":\"" + safeType + "\","
                + "\"status\":\"" + safeStatus + "\","
                + "\"origin\":\"" + safeOrigin + "\","
                + "\"finalPath\":" + finalPath + ","
                + "\"comment\":\"" + escapeJson(fileRecord.getComment()) + "\","
                + "\"occurredAt\":\"" + java.time.LocalDateTime.now() + "\","
                + "\"metadata\":{"
                + "\"failedRecordCount\":" + deferredEvent.failedRecordCount() + ","
                + "\"quarantineFilePath\":" + quarantinePath
                + "}"
                + "}";
    }

    private String safeName(Enum<?> value) {
        return value != null ? value.name() : "UNKNOWN";
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private long elapsedMillis(long startedAtNanos) {
        return (System.nanoTime() - startedAtNanos) / 1_000_000;
    }

    private static final class LockOwnershipException extends IllegalStateException {

        private LockOwnershipException(Long fileRecordId, String instanceId) {
            super("FileRecord " + fileRecordId + " cannot be updated by instance '" + instanceId + "' because it no longer owns the lock");
        }
    }

}
