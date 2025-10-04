package com.com4energy.processor.service;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.com4energy.processor.model.*;
import com.com4energy.processor.repository.FileRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.persistence.EntityNotFoundException;
import static com.com4energy.processor.util.FileUtils.extractExtension;
import static com.com4energy.processor.util.FileUtils.resolveFileType;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileRecordService {

    private final FileRecordRepository repository;

    public FileRecord registerFileAsPendingIntoDatababase(String filename, String originPath, FileOrigin origin) {
        Optional<FileRecord> existing = repository.findByFilenameAndOriginPath(filename, originPath);

        if (existing.isPresent()) {
            if (log.isDebugEnabled()) {
                log.debug("File already exists in the database: {}", existing.get());
            }
            return null;
        }

        String extension = extractExtension(filename);
        FileType type = resolveFileType(extension);

        FileRecord record = FileRecord.builder()
                .filename(filename)
                .originPath(originPath)
                .extension(extension)
                .type(type)
                .origin(origin)
                .status(FileStatus.PENDING)
                .uploadedAt(LocalDateTime.now())
                .build();

        FileRecord saved = repository.save(record);
        log.info("📄 New file registered as pending for processing: {}", saved.getFilename());
        return saved;
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

    @Transactional
    public FileRecord markAsProcessing(FileRecord record) {
        FileRecord managedRecord = repository.findById(record.getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "FileRecord not found with id " + record.getId()));

        managedRecord.setStatus(FileStatus.PROCESSING);
        managedRecord.setLastAttemptAt(LocalDateTime.now());

        return repository.save(managedRecord);
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

    public void markAsRetrying(Long id, FailureReason reason) {
        Optional<FileRecord> record = findById(id);
        if (record.isEmpty()) {
            log.error("❌ FileRecord id={} not found in DB.", id);
            return;
        }
        FileRecord currentRecord = record.get();
        if (currentRecord.getRetryCount() > 3) {
            this.markAsFailedA(currentRecord, reason);
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

    private void markAsFailedA(FileRecord fileRecord, FailureReason reason) {
        fileRecord.setStatus(FileStatus.FAILED);
        fileRecord.setFailureReason(reason);
        fileRecord.setFailedAt(LocalDateTime.now());
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

    public Optional<FileRecord> findByHash(String hash) {
        return repository.findByHash(hash);
    }

    public boolean existsByFilenameAndOriginPath(String filename, String originPath) {
        return repository.existsByFilenameAndOriginPath(filename, originPath);
    }

}
