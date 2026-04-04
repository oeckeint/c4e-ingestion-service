package com.com4energy.processor.service;

import java.io.File;
import java.util.LinkedHashMap;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.com4energy.processor.config.FeatureFlagService;
import com.com4energy.processor.common.IngestionCommonMessageKey;
import com.com4energy.processor.config.properties.features.ProcessorFeatures;
import com.com4energy.processor.outbox.domain.OutboxAggregateType;
import com.com4energy.processor.outbox.domain.OutboxEventType;
import com.com4energy.processor.outbox.service.OutboxService;
import com.com4energy.processor.service.dto.FileRejectedRequest;
import com.com4energy.processor.util.FileUtils;
import com.com4energy.i18n.core.Messages;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.com4energy.processor.model.*;
import com.com4energy.processor.repository.FileRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.persistence.EntityNotFoundException;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileRecordService {

    private final FeatureFlagService featureFlagService;
    private final ProcessorFeatures processorFeatures;
    private final FileRecordRepository fileRecordRepository;
    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;

    @Transactional
    public FileRecord registerFileAsPendingIntoDatabase(String filename, String finalPath, FileOrigin origin, File currentFile, FileRecord fileRecord) {
//        String fileHash = this.hashUtils.computeHashIfEnabled(currentFile);
//
//        Optional<FileRecord> existing = (fileHash != null) ?
//                repository.findByFilenameAndFinalPathOrHash(filename, finalPath, fileHash) :
//                repository.findByFilenameAndFinalPath(filename, finalPath);
//
//        if (existing.isPresent()) {
//            if (log.isDebugEnabled()) {
//                log.debug("File already exists in the database: {}", existing.get());
//            }
//            return null;
//        }

        FileRecord record = FileRecord.builder()
                .filename(filename)
                .finalPath(finalPath)
                .extension(FilenameUtils.getExtension(currentFile.getName()))
                .origin(origin)
                .status(FileStatus.PENDING)
                .hash(fileRecord.getHash())
                .uploadedAt(LocalDateTime.now())
                .retryCount(0)
                .build();

        FileUtils.defineAndSetFileTypeToFileRecord(record, currentFile);

        if (!featureFlagService.isPersistDataEnabled()) {
            log.info("Persist records disabled by feature flag. Ignoring record: {}", record);
            return record;
        }

        FileRecord savedRecord = fileRecordRepository.save(record);

        log.info(Messages.format(IngestionCommonMessageKey.FILE_PENDING_REGISTERED_LOG, savedRecord.getFilename()));
        return savedRecord;
    }

    @Transactional
    public FileRecord saveRejected(FileRejectedRequest request) {
        FailureReason safeReason = Optional.ofNullable(request.reason()).orElse(FailureReason.UNKNOWN_ERROR);
        LocalDateTime now = LocalDateTime.now();

        FileRecord rejectedRecord = FileRecord.builder()
                .filename(request.filename())
                .extension(FilenameUtils.getExtension(request.filename()))
                .origin(FileOrigin.API)
                .finalPath(request.finalPath())
                .status(FileStatus.REJECTED)
                .failureReason(safeReason)
                .uploadedAt(now)
                .failedAt(now)
                .retryCount(0)
                .comment(request.comment())
                .build();

        if (featureFlagService.isPersistDataEnabled()) {
            rejectedRecord = fileRecordRepository.save(rejectedRecord);

            outboxService.saveRejectedFileEvent(
                    OutboxAggregateType.FILE.name(),
                    String.valueOf(rejectedRecord.getId()),
                    OutboxEventType.FILE_REJECTED.name(),
                    buildFileRejectedPayload(rejectedRecord)
            );
        }

        log.info(Messages.format(IngestionCommonMessageKey.FILE_REJECTED_REGISTERED_LOG, rejectedRecord.getFilename(), safeReason.name()));

        return rejectedRecord;
    }


    private String buildFileRejectedPayload(FileRecord savedRecord) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        String sourceId = String.valueOf(savedRecord.getId());
        payload.put("fileId", sourceId);
        payload.put("sourceId", sourceId);
        payload.put("eventType", OutboxEventType.FILE_REJECTED.name());
        payload.put("filename", savedRecord.getFilename());
        payload.put("extension", savedRecord.getExtension());
        payload.put("fileType", savedRecord.getType() != null ? savedRecord.getType().name() : null);
        payload.put("finalPath", savedRecord.getFinalPath());
        payload.put("status", savedRecord.getStatus().name());
        payload.put("origin", savedRecord.getOrigin() != null ? savedRecord.getOrigin().name() : null);
        payload.put("reason", savedRecord.getFailureReason().name());
        payload.put("reasonDescription", savedRecord.getFailureReason().getDescription());
        payload.put("comment", savedRecord.getComment());
        payload.put("createdBy", savedRecord.getCreatedBy());
        payload.put("occurredAt", LocalDateTime.now().toString());

        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize file rejected payload for fileRecordId=" + savedRecord.getId(), e);
        }
    }

    public Optional<FileRecord> findById(Long id) {
        return fileRecordRepository.findById(id);
    }

    @Transactional
    public FileRecord prepareForProcessing(Long id) {
        FileRecord record = fileRecordRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("FileRecord not found: " + id));

        record.setStatus(FileStatus.PROCESSING);
        record.setLastAttemptAt(LocalDateTime.now());

        return record;
    }

    public void markAsDuplicated(Long id, String hash, String finalPath) {
        fileRecordRepository.findById(id).ifPresent(record -> {
            record.setFinalPath(finalPath);
            record.setStatus(FileStatus.DUPLICATED);
            record.setHash(hash);
            record.setFailureReason(FailureReason.DUPLICATED_FILE);
            record.setLastAttemptAt(LocalDateTime.now());
            fileRecordRepository.save(record);
        });
    }

    public FileRecord markAsProcessing(FileRecord record) {
        record.setStatus(FileStatus.PROCESSING);
        record.setLastAttemptAt(LocalDateTime.now());
        return fileRecordRepository.save(record);
    }

    public void markAsProcessed(Long id) {
        fileRecordRepository.findById(id).ifPresent(record -> {
            record.setStatus(FileStatus.PROCESSED);
            record.setProcessedAt(LocalDateTime.now());
            fileRecordRepository.save(record);
        });
    }

    public void markAsProcessed(Long id, String hash, String finalPath) {
        FileRecord record = fileRecordRepository.findById(id).orElseThrow();
        record.setStatus(FileStatus.PROCESSED);
        record.setHash(hash);
        record.setFinalPath(finalPath);
        record.setProcessedAt(LocalDateTime.now());
        fileRecordRepository.save(record);
    }

    @Transactional
    public void markAsProcessed(FileRecord record) {
        record.setStatus(FileStatus.PROCESSED);
        record.setProcessedAt(LocalDateTime.now());
        fileRecordRepository.save(record);
    }

    public void markAsRetrying(Long id, FailureReason reason) {
        Optional<FileRecord> record = findById(id);
        if (record.isEmpty()) {
            log.error("❌ FileRecord id={} not found in DB.", id);
            return;
        }
        FileRecord currentRecord = record.get();
        if (currentRecord.getRetryCount() > this.processorFeatures.getMaxRetries()) {
            this.saveRejected(currentRecord, reason);
            return;
        }
        currentRecord.setStatus(FileStatus.RETRYING);
        currentRecord.setFailureReason(reason);
        currentRecord.setLastAttemptAt(LocalDateTime.now());
        currentRecord.setRetryCount(
                Optional.ofNullable(record.get().getRetryCount()).orElse(0) + 1
        );
        fileRecordRepository.save(currentRecord);
    }

    public void markAsRetrying(FileRecord fileRecord, FailureReason reason) {
        if (fileRecord.getRetryCount() > this.processorFeatures.getMaxRetries()) {
            fileRecord.setComment(String.format("Reached max retries %d", this.processorFeatures.getMaxRetries()));
            this.saveRejected(fileRecord, FailureReason.UNKNOWN_ERROR);
            return;
        }
        fileRecord.setStatus(FileStatus.RETRYING);
        fileRecord.setLastAttemptAt(LocalDateTime.now());
        fileRecord.setRetryCount(
                Optional.ofNullable(fileRecord.getRetryCount()).orElse(0) + 1
        );
        fileRecordRepository.save(fileRecord);
    }

    private void saveRejected(FileRecord fileRecord, FailureReason reason) {
        FailureReason safeReason = Optional.ofNullable(reason).orElse(FailureReason.UNKNOWN_ERROR);
        fileRecord.setStatus(FileStatus.FAILED);
        fileRecord.setLastAttemptAt(LocalDateTime.now());
        fileRecord.setFailedAt(LocalDateTime.now());
        fileRecord.setFailureReason(safeReason);
        if (fileRecord.getComment() == null || fileRecord.getComment().isBlank()) {
            fileRecord.setComment(safeReason.getDescription());
        }
        fileRecordRepository.save(fileRecord);
    }

    public void markAsPending(Long id) {
        fileRecordRepository.findById(id).ifPresent(record -> {
            record.setStatus(FileStatus.PENDING);
            record.setLastAttemptAt(LocalDateTime.now());
            record.setRetryCount(
                    Optional.ofNullable(record.getRetryCount()).orElse(0) + 1
            );
            fileRecordRepository.save(record);
        });
    }

    public void save(FileRecord fileRecord) {
        fileRecordRepository.save(fileRecord);
    }

    public Optional<FileRecord> findByHash(String hash) {
        return fileRecordRepository.findByHash(hash);
    }

    public boolean existsByFilenameAndFinalPath(String filename, String finalPath) {
        return fileRecordRepository.existsByFilenameAndFinalPath(filename, finalPath);
    }

    public boolean existsFileRecordByFilename(String filename) {
        return fileRecordRepository.existsFileRecordByFilename(filename);
    }

    public List<String> findAllFilenamesLike(String name) {
        return fileRecordRepository.findAllFilenamesLike(name);
    }

    public Optional<FileRecord> findFirstByFilenameOrHash(String originalFilename, String fileHash) {
        return this.fileRecordRepository.findFirstByFilenameOrHash(originalFilename, fileHash);
    }

    public List<FileRecord> findAllByStatusIn(List<FileStatus> statuses) {
        return this.fileRecordRepository.findByStatusIn(statuses);
    }

    public List<FileRecord> findAllByStatus(FileStatus status) {
        return this.fileRecordRepository.findByStatus(status);
    }

}
