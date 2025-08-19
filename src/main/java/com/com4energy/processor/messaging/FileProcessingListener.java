package com.com4energy.processor.messaging;

import java.io.File;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import com.com4energy.processor.config.RabbitConfig;
import com.com4energy.processor.model.FileRecord;
import com.com4energy.processor.service.FileProcessingService;
import com.com4energy.processor.service.FileRecordService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class FileProcessingListener {

    private final FileProcessingService fileProcessingService;
    private final FileRecordService fileRecordService;

    public FileProcessingListener(FileProcessingService fileProcessingService, FileRecordService fileRecordService) {
        this.fileProcessingService = fileProcessingService;
        this.fileRecordService = fileRecordService;
    }

    @RabbitListener(queues = RabbitConfig.QUEUE_NAME)
    public void handleFileMessage(Map<String, String> payload) {
        String filePath = Objects.requireNonNull(payload.get("path"), "File path is required");
        String idStr    = Objects.requireNonNull(payload.get("id"), "File ID is required");

        Long fileId;

        try {
            fileId = Long.parseLong(idStr);
        } catch (NumberFormatException ex) {
            log.error("❌ Invalid 'id' format: {}", idStr);
            return;
        }

        File file = new File(filePath);
        if (!file.exists()) {
            log.error("❌ File not found: {}", filePath);
            fileRecordService.markAsFailed(fileId);
            return;
        }

        Optional<FileRecord> opt = fileRecordService.findById(fileId);
        if (opt.isEmpty()) {
            log.error("❌ FileRecord id={} not found in DB.", fileId);
            return;
        }

        FileRecord record = opt.get();

        fileRecordService.markAsProcessing(fileId);
        log.info("📥 Message received from RabbitMQ. Processing: {}", filePath);
        fileProcessingService.processFile(record);

    }

}
