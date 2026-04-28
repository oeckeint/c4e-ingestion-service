package com.com4energy.processor.service.measure.validation;

import com.com4energy.processor.service.measure.MeasureRecord;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Order(10)
public class MandatoryFieldsRecordValidator implements MeasureRecordValidator {

    @Override
    public String brokenRule() {
        return "MANDATORY_FIELDS";
    }

    @Override
    public boolean supports(MeasureRecord record) {
        return true;
    }

    @Override
    public Optional<String> validate(MeasureRecord record) {
        if (record.cups() == null || record.cups().isBlank()) {
            return Optional.of("CUPS vacío");
        }
        if (record.timestamp() == null) {
            return Optional.of("Fecha/hora vacía");
        }
        return Optional.empty();
    }
}
