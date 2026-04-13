package com.com4energy.processor.service.measure.validation;

public record MeasureRecordValidationError(
        int recordIndex,
        String brokenRule,
        String message,
        String rawLine
) {
}
