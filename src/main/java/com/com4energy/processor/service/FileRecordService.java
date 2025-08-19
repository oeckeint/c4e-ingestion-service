package com.com4energy.processor.service;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Service;
import com.com4energy.processor.model.FileOrigin;
import com.com4energy.processor.model.FileRecord;
import com.com4energy.processor.model.FileStatus;
import com.com4energy.processor.model.FileType;
import com.com4energy.processor.repository.FileRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import static com.com4energy.processor.util.FileUtils.extractExtension;
import static com.com4energy.processor.util.FileUtils.resolveFileType;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileRecordService {

    private final FileRecordRepository repository;

    public FileRecord registerFileAsPendingIntoDatababase(String filename, String path, FileOrigin origin, String createdBy) {
        Optional<FileRecord> existing = repository.findByFilenameAndPath(filename, path);

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
                .path(path)
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

}
