package com.com4energy.processor.service.measure.validation;

import com.com4energy.processor.service.measure.MeasureRecord;
import lombok.NonNull;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class MeasureRecordValidationChain {

    private static final int LINE_NUMBER_OFFSET = 1;

    private final List<MeasureRecordValidator> validators;

    public MeasureRecordValidationChain(List<MeasureRecordValidator> validators) {
        this.validators = List.copyOf(validators);
    }

    public MeasureRecordValidationResult validate(@NonNull List<MeasureRecord> records, ValidationMode mode) {
        List<MeasureRecord> validRecords = new ArrayList<>();
        List<MeasureRecordValidationError> errors = new ArrayList<>();

        validators.forEach(validator -> validator.beforeBatch(records));
        try {
            for (int i = 0; i < records.size(); i++) {
                MeasureRecord measureRecord = records.get(i);
                Optional<MeasureRecordValidationError> firstError = firstValidationError(measureRecord, i + LINE_NUMBER_OFFSET);
                if (firstError.isEmpty()) {
                    validRecords.add(measureRecord);
                    continue;
                }

                errors.add(firstError.get());
                if (mode == ValidationMode.FAIL_FAST) {
                    return new MeasureRecordValidationResult(validRecords, errors);
                }
            }
        } finally {
            validators.forEach(MeasureRecordValidator::afterBatch);
        }

        return new MeasureRecordValidationResult(validRecords, errors);
    }

    private Optional<MeasureRecordValidationError> firstValidationError(MeasureRecord measureRecord, int lineNumber) {
        for (MeasureRecordValidator validator : validators) {
            if (!validator.supports(measureRecord)) {
                continue;
            }

            Optional<String> validationError = validator.validate(measureRecord);
            if (validationError.isPresent()) {
                return Optional.of(new MeasureRecordValidationError(
                        lineNumber,
                        validator.brokenRule(),
                        validationError.get(),
                        measureRecord.rawLine()
                ));
            }
        }
        return Optional.empty();
    }
}
