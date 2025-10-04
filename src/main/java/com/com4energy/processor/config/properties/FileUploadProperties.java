package com.com4energy.processor.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import lombok.Data;

@Data
@ConfigurationProperties(prefix = "c4e.upload")
public class FileUploadProperties {

    private String path;
    private String pendingPath;
    private String processedPath;
    private String processingPath;
    private String duplicatesPath;
    private String failedPath;
    private String archivePath;

}
