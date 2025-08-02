package com.com4energy.processor.service;

import com.com4energy.processor.model.FileRecord;
import com.com4energy.processor.model.FileStatus;
import com.com4energy.processor.repository.FileRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileRecordService {

    private final FileRecordRepository repository;

    public FileRecord registerFile(String filename, String path) {

        Optional<FileRecord> existing = repository.findByFilenameAndPath(filename, path);

        if (existing.isPresent()) {
            log.warn("⚠️ Archivo duplicado detectado. Se omite: {}", filename);
            return null;
        }

        FileRecord record = FileRecord.builder()
                .filename(filename)
                .path(path)
                .status(FileStatus.PENDING)
                .uploadedAt(LocalDateTime.now())
                .build();
        return repository.save(record);
    }


    public void markAsProcessing(Long id) {
        repository.findById(id).ifPresent(record -> {
            record.setStatus(FileStatus.PROCESSING);
            record.setLastAttemptAt(LocalDateTime.now());
            repository.save(record);
        });
    }

    public void markAsProcessed(Long id) {
        repository.findById(id).ifPresent(record -> {
            record.setStatus(FileStatus.PROCESSED);
            record.setProcessedAt(LocalDateTime.now());
            repository.save(record);
        });
    }

    public void markAsFailed(Long id) {
        repository.findById(id).ifPresent(record -> {
            record.setStatus(FileStatus.FAILED);
            record.setFailedAt(LocalDateTime.now());
            record.setLastAttemptAt(LocalDateTime.now());
            record.setRetryCount(
                    Optional.ofNullable(record.getRetryCount()).orElse(0) + 1
            );
            repository.save(record);
        });
    }

}
