package com.com4energy.processor.service.measure.validation;

import com.com4energy.processor.service.measure.MeasureRecord;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.regex.Pattern;

@Component
@Order(20)
public class CupsFormatRecordValidator implements MeasureRecordValidator {

    private static final Pattern CUPS_PATTERN = Pattern.compile("^ES[A-Z0-9]{18,20}$");

    @Override
    public String brokenRule() {
        return "CUPS_FORMAT";
    }

    @Override
    public boolean supports(MeasureRecord record) {
        return true;
    }

    @Override
    public Optional<String> validate(MeasureRecord record) {
        String cups = record.cups();
        if (cups == null) {
            return Optional.of("CUPS vacío");
        }

        String normalized = cups.trim().toUpperCase();
        if (!CUPS_PATTERN.matcher(normalized).matches()) {
            return Optional.of("CUPS con formato inválido: " + cups);
        }
        return Optional.empty();
    }
}
