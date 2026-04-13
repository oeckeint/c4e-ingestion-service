package com.com4energy.processor.service.impl;

import java.io.File;
import java.nio.file.Path;

import lombok.NonNull;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import com.com4energy.processor.model.FailureReason;
import com.com4energy.processor.model.FileRecord;
import com.com4energy.processor.service.FileProcessingService;
import com.com4energy.processor.service.FileRecordService;
import com.com4energy.processor.service.IncidentNotificationService;
import com.com4energy.processor.util.FileStorageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileProcessingServiceImpl implements FileProcessingService {

    private final FileRecordService fileRecordService;
    private final FileStorageUtil fileStorageUtil;
    private final IncidentNotificationService incidentNotificationService;

    @Async
    @Override
    public void processFile(@NonNull FileRecord fileRecord) {
        try {
            log.info("✅ Processing file: {}", fileRecord.getOriginalFilename());
            fileRecord.markAsProcessing();
            fileRecordService.save(fileRecord);

            Path processingPath = this.fileStorageUtil.moveFileFromAutomaticToProcessing(new File(fileRecord.getFinalPath()));
            if (processingPath == null) {
                throw new IllegalStateException("Could not move file to processing path");
            }

            File file = processingPath.toFile();

            Path processedPath = this.fileStorageUtil.moveFileFromProcessingToProcessed(file);
            if (processedPath == null) {
                throw new IllegalStateException("Could not move file to processed path");
            }

            fileRecord.markAsSucceded();
            fileRecordService.save(fileRecord);

            log.info("✅ File {} processed", fileRecord.getOriginalFilename());
        } catch (Exception e) {
            log.error("❌ Error processing file {}", fileRecord.getOriginalFilename(), e);
            fileRecord.markAsRetry(FailureReason.UNKNOWN_ERROR);
            fileRecordService.save(fileRecord);
            incidentNotificationService.notifyProcessingError(fileRecord, e);
        }
    }

}
