package com.com4energy.processor.messaging;

import java.io.File;
import java.util.Map;

import com.com4energy.processor.controller.AppFeatureProperties;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class FileProcessingListener {

    private final AppFeatureProperties appFeatureProperties;
    private final FileProcessingService fileProcessingService;
    private final FileRecordService fileRecordService;

    @RabbitListener(queues = RabbitConfig.QUEUE_NAME)
    public void handleFileMessage(Map<String, String> payload) {
        if (!appFeatureProperties.isEnabled("receive-messages")){
            log.warn("⚠️ Rabbit listener is disabled by feature flag. Ignoring message: {}", payload);
            return;
        }

        if (payload == null || payload.get("id") == null || payload.get("path") == null) {
            log.warn("⚠️ Ignoring invalid or empty message: {}", payload);
            return;
        }

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
