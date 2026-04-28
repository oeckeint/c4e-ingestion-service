package com.com4energy.processor.service.measure;

public record MeasureLineParseError(
        int lineNumber,
        String rawLine,
        String message
) {
}

