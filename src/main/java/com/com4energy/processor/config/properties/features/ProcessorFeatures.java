package com.com4energy.processor.config.properties.features;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "processor.features")
public class ProcessorFeatures {

    private int maxRetries;

}
