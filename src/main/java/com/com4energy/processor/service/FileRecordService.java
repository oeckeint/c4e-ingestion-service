package com.com4energy.processor.service;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Set;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;

import com.com4energy.processor.config.FeatureFlagService;
import com.com4energy.processor.exception.DuplicateHashPersistenceException;
import com.com4energy.processor.service.dto.FileHandlingResult;
import com.com4energy.processor.outbox.domain.OutboxAggregateType;
import com.com4energy.processor.outbox.domain.OutboxEventType;
import com.com4energy.processor.outbox.factory.OutboxEventFactory;
import com.com4energy.processor.outbox.repository.OutboxEventRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.apache.commons.io.FilenameUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.com4energy.processor.model.*;
import com.com4energy.processor.repository.FileRecordRepository;
import com.com4energy.processor.service.processing.FileTypeProcessorRegistry;
import com.com4energy.processor.config.properties.FileProcessingJobProperties;
import com.com4energy.processor.config.InstanceIdentifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileRecordService {

    private static final String HASH_UNIQUE_CONSTRAINT_NAME = "idx_hash";
    private static final int MYSQL_DUPLICATE_ENTRY_ERROR_CODE = 1062;

    private final FeatureFlagService feature;
    private final FileRecordRepository repository;
    private final FileTypeProcessorRegistry fileTypeProcessorRegistry;
    private final FileProcessingJobProperties fileProcessingJobProperties;
    private final InstanceIdentifier instanceIdentifier;
    private final OutboxEventRepository outboxEventRepository;

    @Transactional
    public FileHandlingResult saveNew(FileHandlingResult currentResult) {
        return saveNew(currentResult, FileOrigin.API);
    }

    @Transactional
    public FileHandlingResult saveNew(FileHandlingResult currentResult, FileOrigin origin) {
        if (!currentResult.storedInDisk()) {
            return currentResult;
        }

        FileRecord savedRecord = save(FileRecord.from(currentResult.fileContext(), origin), FileStatus.PENDING);

        if (savedRecord != null && savedRecord.getId() != null) {
            return currentResult.withPersistedInFileRecords();
        }

        return currentResult;
    }

    @Transactional
    public FileRecord saveSucceded(FileRecord fileRecordForSaving) {
        return save(fileRecordForSaving, FileStatus.SUCCEEDED);
    }

    protected FileRecord save(FileRecord requestedFileRecord, FileStatus desiredFileStatus) {
        FileStatus originalStatus = requestedFileRecord.getStatus();
        switch (desiredFileStatus) {
            case FAILED -> requestedFileRecord.markAsFailed();
            case NEW -> requestedFileRecord.markAsNew();
            case PENDING -> requestedFileRecord.markAsPending();
            case PROCESSING -> requestedFileRecord.markAsProcessing();
            case REJECTED -> requestedFileRecord.markAsRejected();
            case SUCCEEDED -> requestedFileRecord.markAsSucceded();
            default -> throw new IllegalArgumentException("Unsupported desiredFileStatus: " + desiredFileStatus);
        }

        if (feature.isPersistenceEnabled()) {
            try {
                requestedFileRecord = repository.save(requestedFileRecord);
            } catch (DataIntegrityViolationException ex) {
                if (!isDuplicateHashViolation(ex)) {
                    throw ex;
                }

                throw new DuplicateHashPersistenceException(requestedFileRecord.getHash(), ex);
            }
        }

        publishRegisteredOutboxEventIfNeeded(requestedFileRecord, desiredFileStatus, originalStatus);
        log.info("FileRecord saved: {}", requestedFileRecord);
        return requestedFileRecord;
    }

    private void publishRegisteredOutboxEventIfNeeded(FileRecord fileRecord, FileStatus desiredFileStatus, FileStatus originalStatus) {
        if (desiredFileStatus != FileStatus.PENDING) {
            return;
        }
        // Solo alta inicial: evita publicar en transiciones posteriores a PENDING (ej. retries).
        if (originalStatus != null || fileRecord.getId() == null) {
            return;
        }

        try {
            outboxEventRepository.save(
                    OutboxEventFactory.createPending(
                            OutboxAggregateType.FILE.name(),
                            String.valueOf(fileRecord.getId()),
                            OutboxEventType.FILE_REGISTERED.name(),
                            buildFileRegisteredPayload(fileRecord)
                    )
            );
        } catch (Exception ex) {
            log.warn("Could not publish file-registered outbox event for fileId={}: {}", fileRecord.getId(), ex.getMessage(), ex);
        }
    }

    private String buildFileRegisteredPayload(FileRecord fileRecord) {
        LocalDateTime registeredAt = fileRecord.getUploadedAt() != null ? fileRecord.getUploadedAt() : LocalDateTime.now();
        return "{"
                + "\"fileRecordId\":" + fileRecord.getId() + ","
                + "\"sourceId\":\"" + fileRecord.getId() + "\","
                + "\"eventType\":\"" + OutboxEventType.FILE_REGISTERED.name() + "\","
                + "\"status\":\"" + FileStatus.PENDING.name() + "\","
                + "\"filename\":\"" + escapeJson(fileRecord.getOriginalFilename()) + "\","
                + "\"extension\":\"" + escapeJson(fileRecord.getExtension()) + "\","
                + "\"fileType\":\"" + safeEnumName(fileRecord.getType()) + "\","
                + "\"origin\":\"" + safeEnumName(fileRecord.getOrigin()) + "\","
                + "\"finalPath\":" + nullableJsonValue(fileRecord.getFinalPath()) + ","
                + "\"occurredAt\":\"" + registeredAt + "\""
                + "}";
    }

    private String safeEnumName(Enum<?> value) {
        return value != null ? value.name() : "UNKNOWN";
    }

    private String nullableJsonValue(String value) {
        return value == null ? "null" : "\"" + escapeJson(value) + "\"";
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

    public Optional<FileRecord> findById(Long id) {
        return repository.findById(id);
    }

    public void save(FileRecord fileRecord) {
        repository.save(fileRecord);
    }

    @Transactional
    public boolean saveIfOwnedBy(FileRecord fileRecord, String instanceId) {
        if (fileRecord == null || fileRecord.getId() == null || instanceId == null || instanceId.isBlank()) {
            return false;
        }

        Optional<FileRecord> ownedRecord = repository.findOwnedByIdForUpdate(fileRecord.getId(), instanceId);
        if (ownedRecord.isEmpty()) {
            log.warn("Ownership validation failed while saving FileRecord id={} for instanceId={}", fileRecord.getId(), instanceId);
            return false;
        }

        FileRecord managedRecord = ownedRecord.get();
        copyMutableFields(fileRecord, managedRecord);
        repository.save(managedRecord);
        return true;
    }

    public boolean existsByFilename(String filename) {
        return repository.existsByOriginalFilename(filename);
    }

    public boolean existsByHash(String hash) {
        return repository.existsByHash(hash);
    }

    public Optional<FileRecord> findByHash(String hash) {
        return repository.findByHash(hash);
    }

    public List<FileRecord> findAllByStatus(FileStatus status) {
        return this.repository.findByStatus(status);
    }

    @Transactional
    public void markAsLocked(FileRecord fileRecord, String instanceId) {
        fileRecord.setLocked(true);
        fileRecord.setLockedBy(instanceId);
        fileRecord.setLockedAt(LocalDateTime.now());
        repository.save(fileRecord);
        log.info("FileRecord marked as locked: id={}, lockedBy={}", fileRecord.getId(), instanceId);
    }

    @Transactional
    public void releaseLock(FileRecord fileRecord) {
        fileRecord.setLocked(false);
        fileRecord.setLockedBy(null);
        fileRecord.setLockedAt(null);
        repository.save(fileRecord);
        log.debug("FileRecord lock released: id={}", fileRecord.getId());
    }

    @Transactional
    public boolean releaseLockIfOwnedBy(FileRecord fileRecord, String instanceId) {
        if (fileRecord == null || fileRecord.getId() == null || instanceId == null || instanceId.isBlank()) {
            return false;
        }

        int releasedRows = repository.releaseLockIfOwnedBy(fileRecord.getId(), instanceId);
        if (releasedRows > 0) {
            fileRecord.setLocked(false);
            fileRecord.setLockedBy(null);
            fileRecord.setLockedAt(null);
            log.debug("FileRecord lock released: id={}, lockedBy={}", fileRecord.getId(), instanceId);
            return true;
        }

        log.warn("Lock ownership mismatch. FileRecord id={} requestedBy={}", fileRecord.getId(), instanceId);
        return false;
    }

    public List<FileRecord> findLockedFilesOlderThan(int minutesAgo) {
        LocalDateTime threshold = LocalDateTime.now().minus(minutesAgo, ChronoUnit.MINUTES);
        return repository.findByLockedTrueAndLockedAtBefore(threshold);
    }

    /**
     * Returns true if any sibling file (same base name, different extension) is currently
     * locked/being processed. Used to defer a revision when a related file is in-flight,
     * preventing concurrent writes to the same measure records.
     *
     * Example: if "P2_12345.0" is being processed, "P2_12345.1" will be deferred.
     */
    public boolean isFamilyBeingProcessed(FileRecord fileRecord) {
        if (fileRecord.getId() == null || fileRecord.getOriginalFilename() == null) {
            return false;
        }
        String baseName = FilenameUtils.getBaseName(fileRecord.getOriginalFilename());
        if (baseName == null || baseName.isBlank()) {
            return false;
        }
        String familyPattern = baseName + ".%";
        return repository.existsFamilyFileBeingProcessed(familyPattern, fileRecord.getId(), FileStatus.PROCESSING);
    }


    @Transactional
    public List<FileRecord> claimFilesForProcessingByStatus(FileStatus status) {
        return claimFilesForProcessing(List.of(status));
    }

    private List<FileRecord> claimFilesForProcessing(List<FileStatus> statuses) {

        Set<FileType> supportedTypes = fileTypeProcessorRegistry.supportedTypes();
        int batchSize = fileProcessingJobProperties.getBatchSize();
        String instanceId = instanceIdentifier.getInstanceId();

        List<FileRecord> candidates = repository.findCandidatesForProcessing(
                statuses,
                List.copyOf(supportedTypes),
                PageRequest.of(0, batchSize));

        candidates.forEach(fileRecord -> applyLock(fileRecord, instanceId));
        repository.saveAll(candidates);
        return List.copyOf(candidates);
    }

    @Transactional
    public Optional<FileRecord> claimFileForProcessing(
            Long fileRecordId,
            Set<FileStatus> allowedStatuses,
            Set<FileType> supportedTypes,
            String instanceId
    ) {
        if (fileRecordId == null || allowedStatuses == null || allowedStatuses.isEmpty()
                || supportedTypes == null || supportedTypes.isEmpty()) {
            return Optional.empty();
        }

        return repository.findByIdForUpdate(fileRecordId)
                .filter(fileRecord -> allowedStatuses.contains(fileRecord.getStatus()))
                .filter(fileRecord -> supportedTypes.contains(fileRecord.getType()))
                .filter(fileRecord -> !Boolean.TRUE.equals(fileRecord.getLocked()))
                .map(fileRecord -> {
                    applyLock(fileRecord, instanceId);
                    return repository.save(fileRecord);
                });
    }

    private void applyLock(FileRecord fileRecord, String instanceId) {
        fileRecord.setLocked(true);
        fileRecord.setLockedBy(instanceId);
        fileRecord.setLockedAt(LocalDateTime.now());
    }

    private boolean isDuplicateHashViolation(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (isHashConstraintViolation(current)
                    || isMySqlDuplicateEntry(current)
                    || containsHashConstraintToken(current)) {
                return true;
            }

            current = current.getCause();
        }
        return false;
    }

    private boolean isHashConstraintViolation(Throwable throwable) {
        if (!(throwable instanceof ConstraintViolationException constraintViolationException)) {
            return false;
        }

        return matchesHashConstraint(constraintViolationException.getConstraintName());
    }

    private boolean isMySqlDuplicateEntry(Throwable throwable) {
        if (throwable instanceof SQLIntegrityConstraintViolationException sqlIntegrityConstraintViolationException) {
            return sqlIntegrityConstraintViolationException.getErrorCode() == MYSQL_DUPLICATE_ENTRY_ERROR_CODE;
        }

        if (throwable instanceof SQLException sqlException) {
            return sqlException.getErrorCode() == MYSQL_DUPLICATE_ENTRY_ERROR_CODE;
        }

        return false;
    }

    private boolean containsHashConstraintToken(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null) {
            return false;
        }

        String normalized = message.toLowerCase(Locale.ROOT);
        return normalized.contains(HASH_UNIQUE_CONSTRAINT_NAME)
                || normalized.contains("file_records." + HASH_UNIQUE_CONSTRAINT_NAME);
    }

    private boolean matchesHashConstraint(String constraintName) {
        if (constraintName == null || constraintName.isBlank()) {
            return false;
        }

        String normalized = constraintName.toLowerCase(Locale.ROOT);
        return HASH_UNIQUE_CONSTRAINT_NAME.equalsIgnoreCase(constraintName)
                || normalized.endsWith("." + HASH_UNIQUE_CONSTRAINT_NAME);
    }

    private void copyMutableFields(FileRecord source, FileRecord target) {
        target.setOriginalFilename(source.getOriginalFilename());
        target.setFinalFilename(source.getFinalFilename());
        target.setFinalPath(source.getFinalPath());
        target.setExtension(source.getExtension());
        target.setType(source.getType());
        target.setComment(source.getComment());
        target.setStatus(source.getStatus());
        target.setOrigin(source.getOrigin());
        target.setLocked(source.getLocked());
        target.setLockedBy(source.getLockedBy());
        target.setLockedAt(source.getLockedAt());
        target.setRetryCount(source.getRetryCount());
        target.setProcessedRecords(source.getProcessedRecords());
        target.setDefectedRecords(source.getDefectedRecords());
        target.setParseDurationMs(source.getParseDurationMs());
        target.setProcessingDurationMs(source.getProcessingDurationMs());
        target.setHash(source.getHash());
        target.setFailureReason(source.getFailureReason());
        target.setQualityStatus(source.getQualityStatus());
        target.setBusinessResult(source.getBusinessResult());
        target.setUploadedAt(source.getUploadedAt());
        target.setProcessedAt(source.getProcessedAt());
        target.setFailedAt(source.getFailedAt());
        target.setLastAttemptAt(source.getLastAttemptAt());
    }

}
