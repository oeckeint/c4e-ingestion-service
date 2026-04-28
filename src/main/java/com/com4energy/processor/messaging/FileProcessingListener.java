package com.com4energy.processor.messaging;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.com4energy.processor.config.AppFeatureProperties;
import com.com4energy.processor.config.InstanceIdentifier;
import com.com4energy.processor.model.FileStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import com.com4energy.processor.config.RabbitConfig;
import com.com4energy.processor.service.FileProcessingService;
import com.com4energy.processor.service.FileRecordService;
import com.com4energy.processor.service.processing.FileTypeProcessorRegistry;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileProcessingListener {

    private final AppFeatureProperties appFeatureProperties;
    private final FileProcessingService fileProcessingService;
    private final FileRecordService fileRecordService;
    private final FileTypeProcessorRegistry fileTypeProcessorRegistry;
    private final InstanceIdentifier instanceIdentifier;

    @RabbitListener(queues = RabbitConfig.QUEUE_NAME)
    public void handleFileMessage(Map<String, String> payload) {
        if (!appFeatureProperties.isEnabled("receive-messages")){
            log.warn("RabbitListener feature is disabled. Ignoring message: {}", payload);
            return;
        }

        Optional.ofNullable(payload.get("id"))
                .map(Long::parseLong)
                .flatMap(id -> fileRecordService.claimFileForProcessing(
                        id,
                        Set.of(FileStatus.PENDING),
                        fileTypeProcessorRegistry.supportedTypes(),
                        instanceIdentifier.getInstanceId()
                ))
                .ifPresentOrElse(
                        fileRecord -> {
                            log.info("Processing file: {} from RabbitMQ Queue", fileRecord.getOriginalFilename());
                            fileProcessingService.processFile(fileRecord);
                        },
                        () -> log.warn("Invalid message or FileRecord not found: {}", payload)
                );
    }

}
