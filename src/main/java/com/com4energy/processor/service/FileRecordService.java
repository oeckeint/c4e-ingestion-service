package com.com4energy.processor.service;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.com4energy.processor.config.AppFeatureProperties;
import com.com4energy.processor.config.properties.features.ProcessorFeatures;
import com.com4energy.processor.util.FileRecordUtils;
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

    private final AppFeatureProperties appFeatureProperties;
    private final ProcessorFeatures processorFeatures;
    private final FileRecordRepository repository;

    public FileRecord registerFileAsPendingIntoDatabase(String filename, String originPath, FileOrigin origin, File currentFile, FileRecord fileRecord) {
//        String fileHash = this.hashUtils.computeHashIfEnabled(currentFile);
//
//        Optional<FileRecord> existing = (fileHash != null) ?
//                repository.findByFilenameAndOriginPathOrHash(filename, originPath, fileHash) :
//                repository.findByFilenameAndOriginPath(filename, originPath);
//
//        if (existing.isPresent()) {
//            if (log.isDebugEnabled()) {
//                log.debug("File already exists in the database: {}", existing.get());
//            }
//            return null;
//        }

        FileRecord record = FileRecord.builder()
                .filename(filename)
                .originPath(originPath)
                //.finalPath(currentFile.getAbsolutePath())
                .extension(FilenameUtils.getExtension(currentFile.getName()))
                .origin(origin)
                .status(FileStatus.PENDING)
                .hash(fileRecord.getHash())
                .uploadedAt(LocalDateTime.now())
                .retryCount(0)
                .build();

        FileRecordUtils.defineAndSetFileTypeToFileRecord(record, currentFile);

        if (!appFeatureProperties.isEnabled("persist-data")){
            log.info("Persist records disabled by feature flag. Ignoring record: {}", record);
            return record;
        }

        log.info("📄 New file registered as pending for processing: {}", record.getFilename());
        return repository.save(record);
    }

    public Optional<FileRecord> findById(Long id) {
        return repository.findById(id);
    }

    @Transactional
    public FileRecord prepareForProcessing(Long id) {
        FileRecord record = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("FileRecord not found: " + id));

        record.setStatus(FileStatus.PROCESSING);
        record.setLastAttemptAt(LocalDateTime.now());

        return record;
    }

    public void markAsDuplicated(Long id, String hash, String finalPath) {
        repository.findById(id).ifPresent(record -> {
            record.setFinalPath(finalPath);
            record.setStatus(FileStatus.DUPLICATED);
            record.setHash(hash);
            record.setFailureReason(FailureReason.DUPLICATE_FILE);
            record.setLastAttemptAt(LocalDateTime.now());
            repository.save(record);
        });
    }

    public FileRecord markAsProcessing(FileRecord record) {
        record.setStatus(FileStatus.PROCESSING);
        record.setLastAttemptAt(LocalDateTime.now());
        return repository.save(record);
    }

    public void markAsProcessed(Long id) {
        repository.findById(id).ifPresent(record -> {
            record.setStatus(FileStatus.PROCESSED);
            record.setProcessedAt(LocalDateTime.now());
            repository.save(record);
        });
    }

    public void markAsProcessed(Long id, String hash, String finalPath) {
        FileRecord record = repository.findById(id).orElseThrow();
        record.setStatus(FileStatus.PROCESSED);
        record.setHash(hash);
        record.setFinalPath(finalPath);
        record.setProcessedAt(LocalDateTime.now());
        repository.save(record);
    }

    @Transactional
    public void markAsProcessed(FileRecord record) {
        record.setStatus(FileStatus.PROCESSED);
        record.setProcessedAt(LocalDateTime.now());
        repository.save(record);
    }

    public void markAsRetrying(Long id, FailureReason reason) {
        Optional<FileRecord> record = findById(id);
        if (record.isEmpty()) {
            log.error("❌ FileRecord id={} not found in DB.", id);
            return;
        }
        FileRecord currentRecord = record.get();
        if (currentRecord.getRetryCount() > this.processorFeatures.getMaxRetries()) {
            this.markAsFailed(currentRecord, reason);
            return;
        }
        currentRecord.setStatus(FileStatus.RETRYING);
        currentRecord.setFailureReason(reason);
        currentRecord.setLastAttemptAt(LocalDateTime.now());
        currentRecord.setRetryCount(
                Optional.ofNullable(record.get().getRetryCount()).orElse(0) + 1
        );
        repository.save(currentRecord);
    }

    public void markAsRetrying(FileRecord fileRecord, FailureReason reason) {
        if (fileRecord.getRetryCount() > this.processorFeatures.getMaxRetries()) {
            fileRecord.setComment(String.format("Reached max retries %d", this.processorFeatures.getMaxRetries()));
            this.markAsFailed(fileRecord, FailureReason.UNKNOWN_ERROR);
            return;
        }
        fileRecord.setStatus(FileStatus.RETRYING);
        fileRecord.setLastAttemptAt(LocalDateTime.now());
        fileRecord.setRetryCount(
                Optional.ofNullable(fileRecord.getRetryCount()).orElse(0) + 1
        );
        repository.save(fileRecord);
    }

    public void markAsFailed(FileRecord fileRecord, FailureReason reason) {
        fileRecord.setStatus(FileStatus.FAILED);
        fileRecord.setLastAttemptAt(LocalDateTime.now());
        fileRecord.setFailedAt(LocalDateTime.now());
        fileRecord.setFailureReason(reason);
        repository.save(fileRecord);
    }

    public void markAsPending(Long id) {
        repository.findById(id).ifPresent(record -> {
            record.setStatus(FileStatus.PENDING);
            record.setLastAttemptAt(LocalDateTime.now());
            record.setRetryCount(
                    Optional.ofNullable(record.getRetryCount()).orElse(0) + 1
            );
            repository.save(record);
        });
    }

    public void save(FileRecord fileRecord) {
        repository.save(fileRecord);
    }

    public Optional<FileRecord> findByHash(String hash) {
        return repository.findByHash(hash);
    }

    public boolean existsByFilenameAndOriginPath(String filename, String originPath) {
        return repository.existsByFilenameAndOriginPath(filename, originPath);
    }

    public boolean existsFileRecordByFilename(String filename) {
        return repository.existsFileRecordByFilename(filename);
    }

    public List<String> findAllFilenamesLike(String name) {
        return repository.findAllFilenamesLike(name);
    }

    public Optional<FileRecord> findFirstByFilenameOrHash(String originalFilename, String fileHash) {
        return this.repository.findFirstByFilenameOrHash(originalFilename, fileHash);
    }

    public List<FileRecord> findAllByStatusIn(List<FileStatus> statuses) {
        return this.repository.findByStatusIn(statuses);
    }

    public List<FileRecord> findAllByStatus(FileStatus status) {
        return this.repository.findByStatus(status);
    }

}
