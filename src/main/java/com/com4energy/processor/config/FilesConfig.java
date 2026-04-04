package com.com4energy.processor.config;

import com.com4energy.processor.config.properties.FileUploadProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
public class FilesConfig {

    @Bean
    public Set<String> allowedExtensions(FileUploadProperties properties) {
        return properties.normalizedAllowedExtensions();
    }

}
