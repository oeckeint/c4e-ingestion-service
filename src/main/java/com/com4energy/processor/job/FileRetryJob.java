package com.com4energy.processor.job;

import com.com4energy.processor.config.AppFeatureProperties;
import com.com4energy.processor.config.InstanceIdentifier;
import com.com4energy.processor.config.properties.FileProcessingJobProperties;
import com.com4energy.processor.service.FileRecordService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.com4energy.processor.model.FileRecord;
import com.com4energy.processor.model.FileStatus;
import com.com4energy.processor.service.FileProcessingService;
import com.com4energy.processor.service.processing.FileTypeProcessorRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileRetryJob {

    private final AppFeatureProperties appFeatureProperties;
    private final FileRecordService fileRecordService;
    private final FileProcessingService fileProcessorService;
    private final FileTypeProcessorRegistry fileTypeProcessorRegistry;
    private final InstanceIdentifier instanceIdentifier;
    private final FileProcessingJobProperties fileProcessingJobProperties;

    @Scheduled(fixedDelayString = "${file.retry-interval-ms:60000}")
    public void retryPendingFiles() {
        if (!appFeatureProperties.isEnabled("file-retry-job")){
            return;
        }

        List<FileRecord> claimedFiles = fileRecordService.claimFilesForProcessing(
                List.of(FileStatus.RETRY),
                fileTypeProcessorRegistry.supportedTypes(),
                fileProcessingJobProperties.getBatchSize(),
                instanceIdentifier.getInstanceId()
        );

        for (FileRecord fileRecord : claimedFiles) {
            log.info("Processing file: {} from FileRetryJob", fileRecord.getOriginalFilename());
            fileProcessorService.processFile(fileRecord);
        }
    }

}
