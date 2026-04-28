package com.com4energy.processor.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "file.processing")
public class FileProcessingJobProperties {

    private long intervalMs = 3_000;
    private int batchSize = 10;

}
