package com.com4energy.processor.job;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.com4energy.processor.service.FileScannerService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class FileScannerJob {

    private final FileScannerService scannerService;

    @Scheduled(fixedDelayString = "#{fileScannerProperties.scanIntervalMs}")
    public void runScan() {
        scannerService.scanAndRegisterFiles();
    }

}
