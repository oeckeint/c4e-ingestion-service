package com.com4energy.processor.service.measure.validation;

import com.com4energy.processor.service.measure.MeasureRecord;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class MeasureRecordValidationChain {

    private final List<MeasureRecordValidator> validators;

    public MeasureRecordValidationChain(List<MeasureRecordValidator> validators) {
        this.validators = List.copyOf(validators);
    }

    public MeasureRecordValidationResult validate(List<MeasureRecord> records, ValidationMode mode) {
        List<MeasureRecord> validRecords = new ArrayList<>();
        List<MeasureRecordValidationError> errors = new ArrayList<>();

        for (int i = 0; i < records.size(); i++) {
            MeasureRecord record = records.get(i);
            boolean recordValid = true;

            for (MeasureRecordValidator validator : validators) {
                if (!validator.supports(record)) {
                    continue;
                }
                java.util.Optional<String> validationError = validator.validate(record);
                if (validationError.isPresent()) {
                    errors.add(new MeasureRecordValidationError(i + 1, validator.brokenRule(), validationError.get(), record.rawLine()));
                    recordValid = false;
                    if (mode == ValidationMode.FAIL_FAST) {
                        return new MeasureRecordValidationResult(validRecords, errors);
                    }
                    break;
                }
            }

            if (recordValid) {
                validRecords.add(record);
            }
        }

        return new MeasureRecordValidationResult(validRecords, errors);
    }
}
