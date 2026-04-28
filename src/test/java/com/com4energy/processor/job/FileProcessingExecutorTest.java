package com.com4energy.processor.job;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.com4energy.processor.common.LogsCommonMessageKey;
import com.com4energy.processor.model.FileRecord;
import com.com4energy.processor.model.FileStatus;
import com.com4energy.processor.service.FileProcessingService;
import com.com4energy.processor.service.FileRecordService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FileProcessingExecutorTest {

    private FileRecordService fileRecordService;
    private FileProcessingService fileProcessingService;
    private SimpleMeterRegistry meterRegistry;
    private FileProcessingExecutor executor;

    private Logger logger;
    private Level previousLevel;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        fileRecordService = mock(FileRecordService.class);
        fileProcessingService = mock(FileProcessingService.class);
        meterRegistry = new SimpleMeterRegistry();
        executor = new FileProcessingExecutor(fileRecordService, fileProcessingService, meterRegistry);

        logger = (Logger) LoggerFactory.getLogger(FileProcessingExecutor.class);
        previousLevel = logger.getLevel();
        logger.setLevel(Level.DEBUG);
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(logAppender);
        logger.setLevel(previousLevel);
        logAppender.stop();
        meterRegistry.close();
    }

    @Test
    void executeWhenDisabledSkipsClaimingAndIncrementsSkippedMetric() {
        executor.execute(
            () -> false,
            FileStatus.PENDING,
            LogsCommonMessageKey.FILE_PENDING_JOB_CLAIMED,
            "FileProcessingPendingJob"
        );

        verify(fileRecordService, never()).claimFilesForProcessingByStatus(any());
        verify(fileProcessingService, never()).processFile(any());

        double skipped = meterRegistry.get("c4e.scheduler.job.skipped")
            .tags("job", "FileProcessingPendingJob", "status", "PENDING")
            .counter()
            .count();
        assertEquals(1.0, skipped);
        assertTrue(logsContain("skipped because feature flag is disabled"));
    }

    @Test
    void executeWhenEnabledProcessesFilesAndPublishesSuccessMetrics() {
        FileRecord file1 = mock(FileRecord.class);
        FileRecord file2 = mock(FileRecord.class);
        when(fileRecordService.claimFilesForProcessingByStatus(FileStatus.PENDING)).thenReturn(List.of(file1, file2));

        executor.execute(
            () -> true,
            FileStatus.PENDING,
            LogsCommonMessageKey.FILE_PENDING_JOB_CLAIMED,
            "FileProcessingPendingJob"
        );

        verify(fileRecordService).claimFilesForProcessingByStatus(FileStatus.PENDING);
        verify(fileProcessingService, times(1)).processFile(file1);
        verify(fileProcessingService, times(1)).processFile(file2);

        double claimed = meterRegistry.get("c4e.scheduler.job.claimed")
            .tags("job", "FileProcessingPendingJob", "status", "PENDING")
            .counter()
            .count();
        assertEquals(2.0, claimed);

        var timer = meterRegistry.get("c4e.scheduler.job.duration")
            .tags("job", "FileProcessingPendingJob", "status", "PENDING", "outcome", "success")
            .timer();
        assertNotNull(timer);
        assertEquals(1L, timer.count());
        assertTrue(logsContain("started for status PENDING"));
        assertTrue(logsContain("completed for status PENDING"));
    }

    @Test
    void executeWhenProcessingFailsPublishesFailureMetricsAndRethrows() {
        FileRecord file = mock(FileRecord.class);
        when(fileRecordService.claimFilesForProcessingByStatus(FileStatus.RETRY)).thenReturn(List.of(file));
        RuntimeException boom = new RuntimeException("boom");
        doThrow(boom).when(fileProcessingService).processFile(file);

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> executor.execute(
            () -> true,
            FileStatus.RETRY,
            LogsCommonMessageKey.FILE_RETRY_JOB_CLAIMED,
            "FileProcessingRetryJob"
        ));

        assertEquals(boom, thrown);

        double failed = meterRegistry.get("c4e.scheduler.job.failed")
            .tags("job", "FileProcessingRetryJob", "status", "RETRY")
            .counter()
            .count();
        assertEquals(1.0, failed);

        var errorTimer = meterRegistry.get("c4e.scheduler.job.duration")
            .tags("job", "FileProcessingRetryJob", "status", "RETRY", "outcome", "error")
            .timer();
        assertNotNull(errorTimer);
        assertEquals(1L, errorTimer.count());

        assertNull(meterRegistry.find("c4e.scheduler.job.claimed")
            .tags("job", "FileProcessingRetryJob", "status", "RETRY")
            .counter());
        assertTrue(logsContain("failed for status RETRY"));
    }

    private boolean logsContain(String text) {
        return logAppender.list.stream().anyMatch(event -> event.getFormattedMessage().contains(text));
    }
}
