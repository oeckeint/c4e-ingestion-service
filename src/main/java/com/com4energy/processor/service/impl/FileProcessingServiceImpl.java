package com.com4energy.processor.service.impl;

import java.io.File;
import java.nio.file.Path;

import com.com4energy.processor.config.AppFeatureProperties;
import lombok.NonNull;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import com.com4energy.processor.model.FailureReason;
import com.com4energy.processor.model.FileRecord;
import com.com4energy.processor.service.FileProcessingService;
import com.com4energy.processor.service.FileRecordService;
import com.com4energy.processor.util.FileStorageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileProcessingServiceImpl implements FileProcessingService {

    private final AppFeatureProperties appFeatureProperties;
    private final FileRecordService fileRecordService;
    private final FileStorageUtil fileStorageUtil;

    @Async
    @Override
    public void processFile(@NonNull FileRecord record) {
        try {
            log.info("✅ Processing file: {}", record.getFilename());
            record = fileRecordService.markAsProcessing(record);

            Path processingPath = this.fileStorageUtil.moveFileFromAutomaticToProcessing(new File(record.getOriginPath()));
            record.setFinalPath(processingPath.toAbsolutePath().toString());
            this.fileRecordService.save(record);

            File file = processingPath.toFile();

            Path processedPath = this.fileStorageUtil.moveFileFromProcessingToProcessed(file);
            record.setFinalPath(processedPath.toAbsolutePath().toString());
            fileRecordService.markAsProcessed(record);

            log.info("✅ File {} processed", record.getFilename());
        } catch (Exception e) {
            log.error("❌ Error processing file {}", record.getFilename(), e);
            fileRecordService.markAsRetrying(record, FailureReason.UNKNOWN_ERROR);
        }
    }

}
