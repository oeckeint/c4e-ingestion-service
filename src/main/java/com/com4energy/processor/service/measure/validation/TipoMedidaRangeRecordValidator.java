package com.com4energy.processor.service.measure.validation;

import com.com4energy.processor.service.measure.MeasureRecord;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Order(30)
public class TipoMedidaRangeRecordValidator implements MeasureRecordValidator {

    @Override
    public String brokenRule() {
        return "TIPO_MEDIDA_RANGE";
    }

    @Override
    public boolean supports(MeasureRecord measureRecord) {
        return measureRecord instanceof MeasureRecord.Hourly || measureRecord instanceof MeasureRecord.QuarterHourly;
    }

    @Override
    public Optional<String> validate(MeasureRecord measureRecord) {
        int tipoMedida;
        if (measureRecord instanceof MeasureRecord.Hourly hourly) {
            tipoMedida = hourly.tipoMedida();
        } else if (measureRecord instanceof MeasureRecord.QuarterHourly quarterHourly) {
            tipoMedida = quarterHourly.tipoMedida();
        } else {
            return Optional.empty();
        }

        if (tipoMedida < 0 || tipoMedida > 99) {
            return Optional.of("tipoMedida fuera de rango [0..99]: " + tipoMedida);
        }
        return Optional.empty();
    }
}
