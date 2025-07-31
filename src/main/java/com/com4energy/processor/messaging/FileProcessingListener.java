package com.com4energy.processor.messaging;

import com.com4energy.processor.config.RabbitConfig;
import com.com4energy.processor.service.FileProcessingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Map;

@Slf4j
@Component
public class FileProcessingListener {

    private final FileProcessingService fileProcessingService;

    public FileProcessingListener(FileProcessingService fileProcessingService) {
        this.fileProcessingService = fileProcessingService;
    }

    @RabbitListener(queues = RabbitConfig.QUEUE_NAME)
    public void handleFileMessage(Map<String, String> payload) {
        String filePath = payload.get("path");

        if (filePath == null) {
            log.error("❌ Invalid payload, missing 'path'.");
            return;
        }

        File file = new File(filePath);
        if (!file.exists()) {
            log.error("❌ File not found: {}", filePath);
            return;
        }

        log.info("📥 Message recived from RabbitMQ: {}", filePath);
        fileProcessingService.processFile(file);
    }

}
