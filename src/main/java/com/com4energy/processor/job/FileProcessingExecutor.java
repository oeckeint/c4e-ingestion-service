package com.com4energy.processor.job;

import com.com4energy.i18n.core.Messages;
import com.com4energy.processor.common.LogsCommonMessageKey;
import com.com4energy.processor.model.FileRecord;
import com.com4energy.processor.model.FileStatus;
import com.com4energy.processor.service.FileProcessingService;
import com.com4energy.processor.service.FileRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.BooleanSupplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileProcessingExecutor {

    private final FileRecordService fileRecordService;
    private final FileProcessingService fileProcessingService;

    public void execute(BooleanSupplier isEnabled, FileStatus status, LogsCommonMessageKey logKey) {
        if (!isEnabled.getAsBoolean()) return;

        List<FileRecord> files = fileRecordService.claimFilesForProcessingByStatus(status);
        log.info(Messages.format(logKey, files.size()));
        files.forEach(fileProcessingService::processFile);
    }
}
