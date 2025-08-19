package com.com4energy.processor.service.impl;

import java.io.File;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import com.com4energy.processor.model.FileRecord;
import com.com4energy.processor.service.FileProcessingService;
import com.com4energy.processor.service.FileRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileProcessingServiceImpl implements FileProcessingService {

    private final FileRecordService fileRecordService;

    @Async
    @Override
    public void processFile(FileRecord record) {
        try {

            File file = new File(record.getPath());
            log.info("Processing file: {}", file.getAbsolutePath());

            // Simulación de proceso real
            Thread.sleep(5000);

            //fileRecordService.markAsProcessed(record.getId());
            log.info("File processing completed: {}", file.getName());
        } catch (Exception e) {
            log.error("Processing failed for id={}: {}", record.getId(), e.getMessage(), e);
            fileRecordService.markAsFailed(record.getId());
        }
    }

}
