package com.com4energy.processor.service;

import com.com4energy.processor.config.RabbitConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class MessageProducer {

    private final RabbitTemplate rabbitTemplate;

    public MessageProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendFileMessage(String filename, String filePath) {
        Map<String, String> payload = Map.of(
                "filename", filename,
                "path", filePath
        );

        rabbitTemplate.convertAndSend(
                RabbitConfig.EXCHANGE_NAME,
                RabbitConfig.ROUTING_KEY,
                payload
        );

        log.info("📤 Message sent to RabbitMQ: filename={}, path={}", filename, filePath);
    }

}