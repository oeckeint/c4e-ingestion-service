package com.com4energy.processor.service.measure.validation;

import com.com4energy.processor.service.measure.MeasureRecord;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MeasureRecordValidationChainTest {

    @Test
    void tolerantModeAccumulatesErrorsAndKeepsValidRecords() {
        MeasureRecordValidationChain chain = new MeasureRecordValidationChain(List.of(
                new MandatoryFieldsRecordValidator(),
                new CupsFormatRecordValidator(),
                new TipoMedidaRangeRecordValidator(),
                new TemporalRangeRecordValidator(),
                new NonNegativeMagnitudesRecordValidator()
        ));

        MeasureRecordValidationResult result = chain.validate(
                List.of(validHourly(), invalidCupsHourly()),
                ValidationMode.TOLERANT
        );

        assertEquals(1, result.validRecords().size());
        assertEquals(1, result.errorCount());
        assertTrue(result.errors().get(0).message().contains("CUPS"));
        assertTrue(result.errors().get(0).rawLine().contains("ES001"));
    }

    @Test
    void failFastStopsAtFirstError() {
        MeasureRecordValidationChain chain = new MeasureRecordValidationChain(List.of(
                new MandatoryFieldsRecordValidator(),
                new CupsFormatRecordValidator(),
                new NonNegativeMagnitudesRecordValidator()
        ));

        MeasureRecordValidationResult result = chain.validate(
                List.of(invalidCupsHourly(), invalidNegativeHourly()),
                ValidationMode.FAIL_FAST
        );

        assertEquals(0, result.validRecords().size());
        assertEquals(1, result.errorCount());
        assertTrue(result.errors().get(0).brokenRule().equals("CUPS_FORMAT") || result.errors().get(0).brokenRule().equals("MANDATORY_FIELDS"));
    }

    @Test
    void nonNegativeValidatorFlagsNegativeValues() {
        MeasureRecordValidationChain chain = new MeasureRecordValidationChain(List.of(
                new MandatoryFieldsRecordValidator(),
                new NonNegativeMagnitudesRecordValidator()
        ));

        MeasureRecordValidationResult result = chain.validate(
                List.of(invalidNegativeHourly()),
                ValidationMode.TOLERANT
        );

        assertEquals(0, result.validRecords().size());
        assertEquals(1, result.errorCount());
        assertTrue(result.errors().get(0).message().contains("Valor negativo"));
    }

    @Test
    void invokesBatchLifecycleHooksOnValidators() {
        TrackingValidator validator = new TrackingValidator();
        MeasureRecordValidationChain chain = new MeasureRecordValidationChain(List.of(validator));

        MeasureRecordValidationResult result = chain.validate(List.of(validHourly(), validHourly()), ValidationMode.TOLERANT);

        assertEquals(2, result.validRecords().size());
        assertEquals(1, validator.beforeBatchCalls);
        assertEquals(1, validator.afterBatchCalls);
        assertEquals(2, validator.validateCalls);
    }

    private static final class TrackingValidator implements MeasureRecordValidator {
        private int beforeBatchCalls;
        private int afterBatchCalls;
        private int validateCalls;

        @Override
        public String brokenRule() {
            return "TRACKING";
        }

        @Override
        public boolean supports(MeasureRecord record) {
            return true;
        }

        @Override
        public java.util.Optional<String> validate(MeasureRecord record) {
            validateCalls++;
            return java.util.Optional.empty();
        }

        @Override
        public void beforeBatch(List<MeasureRecord> records) {
            beforeBatchCalls++;
        }

        @Override
        public void afterBatch() {
            afterBatchCalls++;
        }
    }

    private MeasureRecord.Hourly validHourly() {
        return new MeasureRecord.Hourly(
                "ES123456789012345678",
                LocalDateTime.of(2025, 1, 1, 0, 0),
                11,
                0f,
                1f,
                0f,
                0f,
                0f,
                0f,
                0f,
                0f,
                0f,
                0f,
                0f,
                0f,
                0f,
                128f,
                0f,
                128f,
                0f,
                1,
                0,
                "P1D_0021_0894_20240104.0",
                "ES123456789012345678;11;2025/01/01 00:00:00;..."
        );
    }

    private MeasureRecord.Hourly invalidCupsHourly() {
        return new MeasureRecord.Hourly(
                "ES001",
                LocalDateTime.of(2025, 1, 1, 0, 0),
                11,
                0f,
                1f,
                0f,
                0f,
                0f,
                0f,
                0f,
                0f,
                0f,
                0f,
                0f,
                0f,
                0f,
                128f,
                0f,
                128f,
                0f,
                1,
                0,
                "P1D_0021_0894_20240104.0",
                "ES001;11;2025/01/01 00:00:00;..."
        );
    }

    private MeasureRecord.Hourly invalidNegativeHourly() {
        return new MeasureRecord.Hourly(
                "ES123456789012345678",
                LocalDateTime.of(2025, 1, 1, 0, 0),
                11,
                0f,
                -1f,
                0f,
                0f,
                0f,
                0f,
                0f,
                0f,
                0f,
                0f,
                0f,
                0f,
                0f,
                128f,
                0f,
                128f,
                0f,
                1,
                0,
                "P1D_0021_0894_20240104.0",
                "ES123456789012345678;11;2025/01/01 00:00:00;0;-1;..."
        );
    }
}
