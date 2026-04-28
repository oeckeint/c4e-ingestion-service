package com.com4energy.processor.job;

import com.com4energy.processor.config.FeatureFlagService;
import com.com4energy.processor.service.ScannerLockMaintenanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScannerLockMaintenanceJob {

    private final FeatureFlagService featureFlagService;
    private final ScannerLockMaintenanceService scannerLockMaintenanceService;

    @Scheduled(fixedDelayString = "#{fileScannerProperties.lockMaintenanceIntervalMs}")
    public void runMaintenance() {
        if (!featureFlagService.isScannerLockMaintenanceJobEnabled()) {
            return;
        }

        scannerLockMaintenanceService.cleanupExpiredLocks();
    }
}

