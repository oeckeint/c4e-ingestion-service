package com.com4energy.processor.service;

import com.com4energy.processor.config.AppFeatureProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import com.com4energy.processor.config.RabbitConfig;
import com.com4energy.processor.messaging.dto.FileMessage;
import com.com4energy.processor.model.FileRecord;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageProducer {

    private final AppFeatureProperties appFeatureProperties;
    private final RabbitTemplate rabbitTemplate;

    public void sendFileAsMessageToRabbit(FileRecord record) {
        if (!appFeatureProperties.isEnabled("send-messages")){
            log.info("Send messages disabled by feature flag. Ignoring message: {}", record);
            return;
        }

        rabbitTemplate.convertAndSend(
                RabbitConfig.EXCHANGE_NAME,
                RabbitConfig.ROUTING_KEY,
                new FileMessage(record.getId(), record.getOriginPath())
        );

        log.info("📤 File sent to RabbitMQ: id={}, filename={}, path={}",
                record.getId(), record.getFilename(), record.getOriginPath());
    }

}
