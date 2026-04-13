package com.com4energy.processor.service.measure.validation;

import com.com4energy.processor.service.measure.MeasureRecord;

import java.util.Optional;

public interface MeasureRecordValidator {

    String brokenRule();

    boolean supports(MeasureRecord record);

    Optional<String> validate(MeasureRecord record);
}
