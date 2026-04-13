package com.com4energy.processor.job;

import com.com4energy.processor.config.AppFeatureProperties;
import com.com4energy.processor.config.InstanceIdentifier;
import com.com4energy.processor.config.properties.FileProcessingJobProperties;
import com.com4energy.processor.model.FileRecord;
import com.com4energy.processor.model.FileStatus;
import com.com4energy.processor.service.FileProcessingService;
import com.com4energy.processor.service.FileRecordService;
import com.com4energy.processor.service.processing.FileTypeProcessorRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileProcessingJob {

    private final AppFeatureProperties appFeatureProperties;
    private final FileRecordService fileRecordService;
    private final FileProcessingService fileProcessingService;
    private final FileTypeProcessorRegistry fileTypeProcessorRegistry;
    private final InstanceIdentifier instanceIdentifier;
    private final FileProcessingJobProperties fileProcessingJobProperties;

    @Scheduled(fixedDelayString = "#{fileProcessingJobProperties.intervalMs}")
    public void processPendingFiles() {
        if (!appFeatureProperties.isEnabled("file-processing-job")) {
            log.info("FileProcessingJob feature is disabled");
            return;
        }

        List<FileRecord> claimedFiles = fileRecordService.claimFilesForProcessing(
                List.of(FileStatus.PENDING),
                fileTypeProcessorRegistry.supportedTypes(),
                fileProcessingJobProperties.getBatchSize(),
                instanceIdentifier.getInstanceId()
        );

        if (claimedFiles.isEmpty()) {
            return;
        }

        log.info("FileProcessingJob claimed {} file(s) for processing", claimedFiles.size());
        claimedFiles.forEach(fileProcessingService::processFile);
    }
}


