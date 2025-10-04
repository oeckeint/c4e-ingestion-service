package com.com4energy.processor.messaging.dto;

import java.util.Map;
import java.util.Objects;

public record FileMessage(Long id, String path) {

    public FileMessage(Map<String, String> payload) {
        this(parseId(payload.get("id")), require(payload.get("path"), "File path is required"));
    }

    private static String require(String value, String message) {
        return Objects.requireNonNull(value, message);
    }

    private static Long parseId(String idStr) {
        try {
            return Long.parseLong(require(idStr, "File ID is required"));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("❌ Invalid 'id' format: " + idStr, e);
        }
    }

}
