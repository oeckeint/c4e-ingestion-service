package com.com4energy.processor.config.properties;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import lombok.Data;

@ConfigurationProperties(prefix = "scanner")
@Data
@Component("fileScannerProperties")
public class FileScannerProperties {

    private List<String> paths;
    private long scanIntervalMs;

}
