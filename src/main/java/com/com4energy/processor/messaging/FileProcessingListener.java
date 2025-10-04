package com.com4energy.processor.messaging;

import java.io.File;
import java.util.Map;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import com.com4energy.processor.config.RabbitConfig;
import com.com4energy.processor.messaging.dto.FileMessage;
import com.com4energy.processor.model.FailureReason;
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
        FileMessage message = new FileMessage(payload);

        File file = new File(message.path());
        if (!file.exists()) {
            log.error("❌ File not found: {}", message.path());
            fileRecordService.markAsRetrying(message.id(), FailureReason.FILE_NOT_FOUND);
            return;
        }

        try {
            log.info("📥 Message received from RabbitMQ. Processing: {}", message.path());
            fileProcessingService.processFile(
                    fileRecordService.prepareForProcessing(message.id())
            );
        } catch (IllegalArgumentException e) {
            log.error(e.getMessage());
        }

    }

}
