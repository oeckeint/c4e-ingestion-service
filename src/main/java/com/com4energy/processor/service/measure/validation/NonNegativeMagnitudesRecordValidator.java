package com.com4energy.processor.service.measure.validation;

import com.com4energy.processor.service.measure.MeasureRecord;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Order(50)
public class NonNegativeMagnitudesRecordValidator implements MeasureRecordValidator {

    private static final String FIELD_ACTENT = "actent";
    private static final String FIELD_BANDERA_INV_VER = "banderaInvVer";

    @Override
    public String brokenRule() {
        return "NON_NEGATIVE_MAGNITUDES";
    }

    @Override
    public boolean supports(MeasureRecord measureRecord) {
        return true;
    }

    @Override
    public Optional<String> validate(MeasureRecord measureRecord) {
        if (measureRecord instanceof MeasureRecord.Hourly hourly) {
            return firstNegative(
                    value(hourly.actent(), FIELD_ACTENT),
                    value(hourly.qactent(), "qactent"),
                    value(hourly.actsal(), "actsal"),
                    value(hourly.qactsal(), "qactsal"),
                    value(hourly.rQ1(), "rQ1"),
                    value(hourly.qrQ1(), "qrQ1"),
                    value(hourly.rQ2(), "rQ2"),
                    value(hourly.qrQ2(), "qrQ2"),
                    value(hourly.rQ3(), "rQ3"),
                    value(hourly.qrQ3(), "qrQ3"),
                    value(hourly.rQ4(), "rQ4"),
                    value(hourly.qrQ4(), "qrQ4"),
                    value(hourly.medres1(), "medres1"),
                    value(hourly.qmedres1(), "qmedres1"),
                    value(hourly.medres2(), "medres2"),
                    value(hourly.qmedres2(), "qmedres2")
            );
        }

        if (measureRecord instanceof MeasureRecord.QuarterHourly quarterHourly) {
            return firstNegative(
                    value(quarterHourly.banderaInvVer(), FIELD_BANDERA_INV_VER),
                    value(quarterHourly.actent(), FIELD_ACTENT),
                    value(quarterHourly.qactent(), "qactent"),
                    value(quarterHourly.actsal(), "actsal"),
                    value(quarterHourly.qactsal(), "qactsal"),
                    value(quarterHourly.rQ1(), "rQ1"),
                    value(quarterHourly.qrQ1(), "qrQ1"),
                    value(quarterHourly.rQ2(), "rQ2"),
                    value(quarterHourly.qrQ2(), "qrQ2"),
                    value(quarterHourly.rQ3(), "rQ3"),
                    value(quarterHourly.qrQ3(), "qrQ3"),
                    value(quarterHourly.rQ4(), "rQ4"),
                    value(quarterHourly.qrQ4(), "qrQ4"),
                    value(quarterHourly.medres1(), "medres1"),
                    value(quarterHourly.qmedres1(), "qmedres1"),
                    value(quarterHourly.medres2(), "medres2"),
                    value(quarterHourly.qmedres2(), "qmedres2")
            );
        }

        if (measureRecord instanceof MeasureRecord.Legacy legacy) {
            return firstNegative(
                    value(legacy.banderaInvVer(), FIELD_BANDERA_INV_VER),
                    value(legacy.ae1(), "ae1"),
                    value(legacy.as1(), "as1"),
                    value(legacy.rq1(), "rq1"),
                    value(legacy.rq2(), "rq2"),
                    value(legacy.rq3(), "rq3"),
                    value(legacy.rq4(), "rq4"),
                    value(legacy.metodObt(), "metodObt"),
                    value(legacy.indicFirmez(), "indicFirmez")
            );
        }

        if (measureRecord instanceof MeasureRecord.Cch cch) {
            return firstNegative(
                    value(cch.banderaInvVer(), FIELD_BANDERA_INV_VER),
                    value(cch.actent(), FIELD_ACTENT),
                    value(cch.metod(), "metod")
            );
        }

        return Optional.empty();
    }

    private Optional<String> firstNegative(NumericField... fields) {
        for (NumericField field : fields) {
            if (field.value() < 0d) {
                return Optional.of("Valor negativo no permitido en " + field.name() + ": " + field.value());
            }
        }
        return Optional.empty();
    }

    private NumericField value(double value, String name) {
        return new NumericField(value, name);
    }

    private record NumericField(double value, String name) {
    }
}
