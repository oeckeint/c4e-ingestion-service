package com.com4energy.processor.job;

import com.com4energy.processor.config.AppFeatureProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.com4energy.processor.service.FileScannerService;
import lombok.RequiredArgsConstructor;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileScannerJob {

    private final AppFeatureProperties appFeatureProperties;
    private final FileScannerService scannerService;

    @Scheduled(fixedDelayString = "#{fileScannerProperties.scanIntervalMs}")
    public void runScan() {
        if (!appFeatureProperties.isEnabled("scan-files")){
            log.warn("scanner job is disabled");
            return;
        }
        scannerService.scanAndRegisterFiles();
    }

}
