package com.com4energy.processor.service;

import java.util.List;
import java.util.Optional;
import com.com4energy.processor.config.FeatureFlagService;
import com.com4energy.processor.service.dto.FileHandlingResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.com4energy.processor.model.*;
import com.com4energy.processor.repository.FileRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileRecordService {

    private final FeatureFlagService feature;
    private final FileRecordRepository repository;

    @Transactional
    public FileHandlingResult saveNew(FileHandlingResult currentResult) {
        if (!currentResult.storedInDisk()) {
            return currentResult;
        }

        FileRecord savedRecord = save(FileRecord.from(currentResult.fileContext()), FileStatus.NEW);

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
        switch (desiredFileStatus) {
            case FAILED -> requestedFileRecord.markAsFailed();
            case NEW -> requestedFileRecord.markAsNew();
            case PROCESSING -> requestedFileRecord.markAsProcessing();
            case REJECTED -> requestedFileRecord.markAsRejected();
            case SUCCEEDED -> requestedFileRecord.markAsSucceded();
            default -> throw new IllegalArgumentException("Unsupported desiredFileStatus: " + desiredFileStatus);
        }

        if (feature.isPersistDataEnabled()) {
            requestedFileRecord = repository.save(requestedFileRecord);
        }

        log.info("FileRecord saved: {}", requestedFileRecord);
        return requestedFileRecord;
    }

    public Optional<FileRecord> findById(Long id) {
        return repository.findById(id);
    }

    public void save(FileRecord fileRecord) {
        repository.save(fileRecord);
    }

    public boolean existsByFilename(String filename) {
        return repository.existsByOriginalFilename(filename);
    }

    public boolean existsByHash(String hash) {
        return repository.existsByHash(hash);
    }

    public List<FileRecord> findAllByStatus(FileStatus status) {
        return this.repository.findByStatus(status);
    }

}
