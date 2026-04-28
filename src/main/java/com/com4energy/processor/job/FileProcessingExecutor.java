package com.com4energy.processor.job;

import com.com4energy.i18n.core.Messages;
import com.com4energy.processor.common.LogsCommonMessageKey;
import com.com4energy.processor.model.FileRecord;
import com.com4energy.processor.model.FileStatus;
import com.com4energy.processor.service.FileProcessingService;
import com.com4energy.processor.service.FileRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.function.BooleanSupplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileProcessingExecutor {

    private final FileRecordService fileRecordService;
    private final FileProcessingService fileProcessingService;
    private final MeterRegistry meterRegistry;

    public void execute(BooleanSupplier isEnabled, FileStatus status, LogsCommonMessageKey logKey, String jobName) {
        long startNanos = System.nanoTime();
        String statusName = status.name();

        if (!isEnabled.getAsBoolean()) {
            log.debug(Messages.format(LogsCommonMessageKey.FILE_PROCESSING_JOB_DISABLED, jobName, statusName));
            meterRegistry.counter("c4e.scheduler.job.skipped", "job", jobName, "status", statusName).increment();
            return;
        }

        log.debug(Messages.format(LogsCommonMessageKey.FILE_PROCESSING_JOB_STARTED, jobName, statusName));

        try {
            List<FileRecord> files = fileRecordService.claimFilesForProcessingByStatus(status);
            int claimedCount = files.size();

            if (claimedCount > 0) {
                log.info(Messages.format(logKey, claimedCount));
                files.forEach(fileProcessingService::processFile);

                long durationMs = Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
                log.info(Messages.format(
                    LogsCommonMessageKey.FILE_PROCESSING_JOB_COMPLETED,
                    jobName,
                    statusName,
                    claimedCount,
                    durationMs,
                    Thread.currentThread().getName()
                ));

                meterRegistry.counter("c4e.scheduler.job.claimed", "job", jobName, "status", statusName)
                    .increment(claimedCount);
                Timer.builder("c4e.scheduler.job.duration")
                    .tag("job", jobName)
                    .tag("status", statusName)
                    .tag("outcome", "success")
                    .register(meterRegistry)
                    .record(Duration.ofNanos(System.nanoTime() - startNanos));
            }

        } catch (RuntimeException ex) {
            long durationMs = Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
            log.error(Messages.format(
                LogsCommonMessageKey.FILE_PROCESSING_JOB_FAILED,
                jobName,
                statusName,
                durationMs,
                ex.getClass().getSimpleName()
            ), ex);

            meterRegistry.counter("c4e.scheduler.job.failed", "job", jobName, "status", statusName).increment();
            Timer.builder("c4e.scheduler.job.duration")
                .tag("job", jobName)
                .tag("status", statusName)
                .tag("outcome", "error")
                .register(meterRegistry)
                .record(Duration.ofNanos(System.nanoTime() - startNanos));
            throw ex;
        }
    }
}
