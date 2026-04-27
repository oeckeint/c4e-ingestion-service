package com.com4energy.processor.service.measure.validation;

import com.com4energy.processor.common.MeasureFieldNames;
import com.com4energy.processor.service.measure.MeasureRecord;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Order(50)
public class NonNegativeMagnitudesRecordValidator implements MeasureRecordValidator {

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
        return switch (measureRecord) {
            case MeasureRecord.Hourly hourly -> firstNegative(extractP1Fields(hourly));
            case MeasureRecord.QuarterHourly quarterHourly -> firstNegative(extractP2Fields(quarterHourly));
            case MeasureRecord.Cch cch -> firstNegative(extractCchFields(cch));
        };
    }

    private NumericField[] extractP1Fields(MeasureRecord.Hourly hourly) {
        return new NumericField[]{
            value(hourly.actent(), MeasureFieldNames.ACTENT),
            value(hourly.qactent(), MeasureFieldNames.QACTENT),
            value(hourly.actsal(), MeasureFieldNames.ACTSAL),
            value(hourly.qactsal(), MeasureFieldNames.QACTSAL),
            value(hourly.rQ1(), MeasureFieldNames.RQ1),
            value(hourly.qrQ1(), MeasureFieldNames.QRQ1),
            value(hourly.rQ2(), MeasureFieldNames.RQ2),
            value(hourly.qrQ2(), MeasureFieldNames.QRQ2),
            value(hourly.rQ3(), MeasureFieldNames.RQ3),
            value(hourly.qrQ3(), MeasureFieldNames.QRQ3),
            value(hourly.rQ4(), MeasureFieldNames.RQ4),
            value(hourly.qrQ4(), MeasureFieldNames.QRQ4),
            value(hourly.medres1(), MeasureFieldNames.MEDRES1),
            value(hourly.qmedres1(), MeasureFieldNames.QMEDRES1),
            value(hourly.medres2(), MeasureFieldNames.MEDRES2),
            value(hourly.qmedres2(), MeasureFieldNames.QMEDRES2)
        };
    }

    private NumericField[] extractP2Fields(MeasureRecord.QuarterHourly quarterHourly) {
        return new NumericField[]{
            value(quarterHourly.banderaInvVer(), MeasureFieldNames.P2_BANDERA_INV_VER),
            value(quarterHourly.actent(), MeasureFieldNames.ACTENT),
            value(quarterHourly.qactent(), MeasureFieldNames.QACTENT),
            value(quarterHourly.actsal(), MeasureFieldNames.ACTSAL),
            value(quarterHourly.qactsal(), MeasureFieldNames.QACTSAL),
            value(quarterHourly.rQ1(), MeasureFieldNames.RQ1),
            value(quarterHourly.qrQ1(), MeasureFieldNames.QRQ1),
            value(quarterHourly.rQ2(), MeasureFieldNames.RQ2),
            value(quarterHourly.qrQ2(), MeasureFieldNames.QRQ2),
            value(quarterHourly.rQ3(), MeasureFieldNames.RQ3),
            value(quarterHourly.qrQ3(), MeasureFieldNames.QRQ3),
            value(quarterHourly.rQ4(), MeasureFieldNames.RQ4),
            value(quarterHourly.qrQ4(), MeasureFieldNames.QRQ4),
            value(quarterHourly.medres1(), MeasureFieldNames.MEDRES1),
            value(quarterHourly.qmedres1(), MeasureFieldNames.QMEDRES1),
            value(quarterHourly.medres2(), MeasureFieldNames.MEDRES2),
            value(quarterHourly.qmedres2(), MeasureFieldNames.QMEDRES2)
        };
    }

    private NumericField[] extractCchFields(MeasureRecord.Cch cch) {
        return new NumericField[]{
            value(cch.banderaInvVer(), MeasureFieldNames.CCH_BANDERA_INV_VER),
            value(cch.actent(), MeasureFieldNames.CCH_ACTENT),
            value(cch.metod(), MeasureFieldNames.CCH_METOD)
        };
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
