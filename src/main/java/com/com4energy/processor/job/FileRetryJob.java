package com.com4energy.processor.job;

import com.com4energy.processor.config.AppFeatureProperties;
import com.com4energy.processor.service.FileRecordService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.com4energy.processor.model.FileRecord;
import com.com4energy.processor.model.FileStatus;
import com.com4energy.processor.service.FileProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileRetryJob {

    private final AppFeatureProperties appFeatureProperties;
    private final FileRecordService fileRecordService;
    private final FileProcessingService fileProcessorService;

    @Scheduled(fixedDelayString = "${file.retry-interval-ms:60000}")
    public void retryPendingFiles() {
        if (!appFeatureProperties.isEnabled("file-retry-job")){
            log.warn("FileRetryJob feature is disabled");
            return;
        }
        for (FileRecord record : fileRecordService.findAllByStatus(FileStatus.RETRYING)) {
            log.info("Processing file: {} from FileRetryJob", record.getFilename());
            fileProcessorService.processFile(record);
        }
    }

}
