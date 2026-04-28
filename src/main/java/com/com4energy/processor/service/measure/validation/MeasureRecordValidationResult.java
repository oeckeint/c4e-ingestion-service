package com.com4energy.processor.service.measure.validation;

import com.com4energy.processor.service.measure.MeasureRecord;

import java.util.List;

public record MeasureRecordValidationResult(
        List<MeasureRecord> validRecords,
        List<MeasureRecordValidationError> errors
) {
    public MeasureRecordValidationResult {
        validRecords = List.copyOf(validRecords);
        errors = List.copyOf(errors);
    }

    public int errorCount() {
        return errors.size();
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}

