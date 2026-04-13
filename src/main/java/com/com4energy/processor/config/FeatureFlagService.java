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

    public boolean isPersistDataEnabled() {
        return isEnabled("persist-data");
    }

    public boolean isNotifyOnErrorEnabled() {
        return isEnabled("notify-on-error");
    }

}
