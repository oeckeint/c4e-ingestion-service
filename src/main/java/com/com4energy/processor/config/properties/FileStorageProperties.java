package com.com4energy.processor.config.properties;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class FileStorageProperties {

    @Value("${c4e.upload.path}")
    private String uploadPath;

}
