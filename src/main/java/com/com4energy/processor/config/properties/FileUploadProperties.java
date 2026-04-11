package com.com4energy.processor.config.properties;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

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
        String rejectedPath,
        String archivePath,
        String automaticPath,
        @Positive long maxSizeBytes,
        @NotEmpty List<@NotEmpty String> allowedExtensions,
        @NotEmpty List<@NotEmpty String> allowedContentTypes
) {

    public Set<String> normalizedAllowedExtensions() {
        return allowedExtensions.stream()
                .map(ext -> ext.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    public Set<String> normalizedAllowedContentTypes() {
        return allowedContentTypes.stream()
                .map(contentType -> contentType.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

}
