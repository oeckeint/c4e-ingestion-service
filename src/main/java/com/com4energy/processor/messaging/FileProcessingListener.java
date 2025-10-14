package com.com4energy.processor.messaging;

import java.util.Map;
import java.util.Optional;

import com.com4energy.processor.config.AppFeatureProperties;
import com.com4energy.processor.model.FileStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import com.com4energy.processor.config.RabbitConfig;
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
            log.warn("RabbitListener feature is disabled. Ignoring message: {}", payload);
            return;
        }

        Optional.ofNullable(payload.get("id"))
                .map(Long::parseLong)
                .flatMap(fileRecordService::findById)
                .filter(record -> record.getStatus() == FileStatus.PENDING)
                .ifPresentOrElse(
                        record -> {
                            log.info("Processing file: {} from RabbitMQ Queue", record.getFilename());
                            fileProcessingService.processFile(record);
                        },
                        () -> log.warn("Invalid message or FileRecord not found: {}", payload)
                );
    }

}
