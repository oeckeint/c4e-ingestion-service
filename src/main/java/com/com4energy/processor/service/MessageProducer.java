package com.com4energy.processor.service;

import java.util.Map;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import com.com4energy.processor.config.RabbitConfig;
import com.com4energy.processor.model.FileRecord;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class MessageProducer {

    private final RabbitTemplate rabbitTemplate;

    public MessageProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendFileAsMessageToRabbit(FileRecord record) {
        Map<String, String> payload = Map.of(
                "id", record.getId().toString(),
                "filename", record.getFilename(),
                "path", record.getPath()
        );

        rabbitTemplate.convertAndSend(
                RabbitConfig.EXCHANGE_NAME,
                RabbitConfig.ROUTING_KEY,
                payload
        );

        log.info("📤 File sent to RabbitMQ: id={}, filename={}, path={}",
                record.getId(), record.getFilename(), record.getPath());
    }

}
