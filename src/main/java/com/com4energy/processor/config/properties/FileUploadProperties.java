package com.com4energy.processor.config.properties;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "c4e.upload")
public record FileUploadProperties(
        String basePath,
        String pendingPath,
        String processedPath,
        String processingPath,
        String duplicatesPath,
        String failedPath,
        String archivePath,
        String automaticPath,
        @Positive long maxSizeBytes,
        @NotEmpty List<@NotEmpty String> allowedExtensions
) {}
