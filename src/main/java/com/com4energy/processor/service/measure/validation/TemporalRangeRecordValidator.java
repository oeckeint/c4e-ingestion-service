package com.com4energy.processor.service.measure.validation;

import com.com4energy.processor.service.measure.MeasureRecord;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Order(40)
public class TemporalRangeRecordValidator implements MeasureRecordValidator {

	@Override
	public String brokenRule() {
		return "TEMPORAL_RANGE";
	}

	@Override
	public boolean supports(MeasureRecord measureRecord) {
		return measureRecord instanceof MeasureRecord.Hourly || measureRecord instanceof MeasureRecord.QuarterHourly;
	}

	@Override
	public Optional<String> validate(MeasureRecord measureRecord) {
		Integer temporal;
		if (measureRecord instanceof MeasureRecord.Hourly hourly) {
			temporal = hourly.temporal();
		} else if (measureRecord instanceof MeasureRecord.QuarterHourly quarterHourly) {
			temporal = quarterHourly.temporal();
		} else {
			return Optional.empty();
		}

		if (temporal == null) {
			return Optional.empty();
		}

		if (temporal < 0 || temporal > 99) {
			return Optional.of("temporal fuera de rango [0..99]: " + temporal);
		}
		return Optional.empty();
	}
}
