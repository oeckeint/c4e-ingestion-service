package com.com4energy.processor.messaging;

import java.io.File;
import java.util.Map;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import com.com4energy.processor.config.RabbitConfig;
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
        String filePath = payload.get("path");
        Long fileId = Long.parseLong(payload.get("id"));

        if (filePath == null) {
            log.error("❌ Invalid payload. 'id' or 'path' missing.");
            return;
        }

        File file = new File(filePath);
        if (!file.exists()) {
            log.error("❌ File not found: {}", filePath);
            fileRecordService.markAsFailed(fileId);
            return;
        }

        fileRecordService.markAsProcessing(fileId);
        try {
            log.info("📥 Message received from RabbitMQ. Processing: {}", filePath);
            fileProcessingService.processFile(file);
            // fileRecordService.markAsProcessed(fileId); // TODO: 🚧 implementar actualización de estado
            throw new UnsupportedOperationException("markAsProcessed aún no está implementado.");
            //fileRecordService.markAsProcessed(fileId);
        } catch (Exception e) {
            log.error("❌ Error processing file: {}", filePath, e);
            fileRecordService.markAsFailed(fileId);
        }

    }

}
