package com.com4energy.processor.controller;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.feature")
public class AppFeatureProperties {

    private Map<String, Boolean> enabled = new HashMap<>();

    public Boolean isEnabled(String key) {
        return enabled.getOrDefault(key, false);
    }

}
