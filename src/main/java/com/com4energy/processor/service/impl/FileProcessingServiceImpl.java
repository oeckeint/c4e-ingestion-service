package com.com4energy.processor.service.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;

import com.com4energy.processor.config.AppFeatureProperties;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import com.com4energy.processor.model.FailureReason;
import com.com4energy.processor.model.FileRecord;
import com.com4energy.processor.model.FileStatus;
import com.com4energy.processor.service.FileProcessingService;
import com.com4energy.processor.service.FileRecordService;
import com.com4energy.processor.util.FileStorageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import static com.com4energy.processor.util.HashUtils.calculateHash;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileProcessingServiceImpl implements FileProcessingService {

    private final AppFeatureProperties appFeatureProperties;
    private final FileRecordService fileRecordService;
    private final FileStorageUtil fileStorageUtil;
    private String hash;

    @Async
    @Override
    public void processFile(FileRecord record) {
        try {
            File file = new File(record.getOriginPath());

            if (isDuplicate(record, file)) return;

            Path processingPath = this.fileStorageUtil.moveFileFromAutomaticToProcessing(file);
            record.setFinalPath(processingPath.toAbsolutePath().toString());
            record = fileRecordService.markAsProcessing(record);
            file = processingPath.toFile();

            Path processedPath = this.fileStorageUtil.moveFileFromProcessingToProcessed(file);
            record.setFinalPath(processedPath.toAbsolutePath().toString());
            fileRecordService.markAsProcessed(record);

            log.info("✅ File {} processed", file.getName());
        } catch (Exception e) {
            log.error("❌ Error processing file {}", record.getFilename(), e);
            fileRecordService.markAsRetrying(record.getId(), FailureReason.UNKNOWN_ERROR);
        }
    }

    private boolean isDuplicate(FileRecord record, File file) throws IOException, NoSuchAlgorithmException {
        FileStatus currentStatus = record.getStatus();
        if (currentStatus.equals(FileStatus.PENDING) ||
                currentStatus.equals(FileStatus.PROCESSING) ||
                currentStatus.equals(FileStatus.RETRYING)) {
            return false;
        }

        // 1. Verificación rápida por nombre y ruta
        if (fileRecordService.existsByFilenameAndOriginPath(record.getFilename(), record.getOriginPath())) {
            log.warn("⚠️ File {} already exists by name/path.", record.getFilename());
            fileRecordService.markAsDuplicated(record.getId(), null, record.getOriginPath());
            fileStorageUtil.moveFileToDuplicates(file);
            return true;
        }

        // 2. Verificación por hash (solo si pasó la primera)
        this.hash = calculateHash(file);
        if (checkIfHashExists(this.hash)) {
            log.warn("⚠️ File {} detected as duplicate by hash.", record.getFilename());
            fileRecordService.markAsDuplicated(record.getId(), this.hash, file.getAbsolutePath());
            fileStorageUtil.moveFileToDuplicates(file);
            return true;
        }
        return false;
    }

    private boolean checkIfHashExists(String hash) {
        return fileRecordService.findByHash(hash).isPresent();
    }

}
