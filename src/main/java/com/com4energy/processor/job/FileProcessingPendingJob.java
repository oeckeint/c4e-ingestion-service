package com.com4energy.processor.job;

import com.com4energy.processor.common.LogsCommonMessageKey;
import com.com4energy.processor.config.FeatureFlagService;
import com.com4energy.processor.model.FileStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileProcessingPendingJob {

    private final FileProcessingExecutor executor;
    private final FeatureFlagService flagsService;

    @Scheduled(fixedDelayString = "#{fileProcessingJobProperties.intervalMs}")
    public void processPendingFiles() {
        executor.execute(
            flagsService::isFileProcessingPendingJob,
            FileStatus.PENDING,
            LogsCommonMessageKey.FILE_PENDING_JOB_CLAIMED
        );
    }

}
