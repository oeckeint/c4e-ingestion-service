package com.com4energy.processor.config;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FeatureFlagService {

    private final AppFeatureProperties appFeatureProperties;

    public boolean isEnabled(String key) {
        return appFeatureProperties.isEnabled(key);
    }

    public boolean isPersistenceEnabled() {
        return isEnabled("persist-data");
    }

    public boolean isNotifyOnErrorEnabled() {
        return isEnabled("notify-on-error");
    }

    public boolean isFileScannerJobEnabled() {
        return isEnabled("file-scanner-job");
    }

    public boolean isScannerLockMaintenanceJobEnabled() {
        return isEnabled("scanner-lock-maintenance-job");
    }

    public boolean isFileProcessingPendingJob() {
        return isEnabled("file-processing-job");
    }

    public boolean isFileProcessingRetryJob() {
        return isEnabled("file-retry-job");
    }

}
