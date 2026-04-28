package com.com4energy.processor.service.measure;

import java.util.List;

public record MeasureParseResult(
        MeasureFilenameMetadata metadata,
        List<MeasureRecord> records,
        List<MeasureLineParseError> errors
) {

    public MeasureParseResult {
        records = List.copyOf(records);
        errors = List.copyOf(errors);
    }

    public int successCount() {
        return records.size();
    }

    public int errorCount() {
        return errors.size();
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}

