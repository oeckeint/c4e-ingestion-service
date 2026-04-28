package com.com4energy.processor.job;

import com.com4energy.processor.common.LogsCommonMessageKey;
import com.com4energy.processor.common.SchedulerBeanNames;
import com.com4energy.processor.config.FeatureFlagService;
import com.com4energy.processor.model.FileStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileProcessingRetryJob {

    private static final String JOB_NAME = "FileProcessingRetryJob";

    private final FileProcessingExecutor executor;
    private final FeatureFlagService flagsService;

    @Scheduled(
        fixedDelayString = "#{fileProcessingJobProperties.intervalMs}",
        scheduler = SchedulerBeanNames.RETRY_JOB_SCHEDULER_BEAN
    )
    public void retryPendingFiles() {
        executor.execute(
            flagsService::isFileProcessingRetryJob,
            FileStatus.RETRY,
            LogsCommonMessageKey.FILE_RETRY_JOB_CLAIMED,
            JOB_NAME
        );
    }

}
