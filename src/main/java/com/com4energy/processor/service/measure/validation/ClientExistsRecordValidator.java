package com.com4energy.processor.service.measure.validation;

import com.com4energy.processor.repository.ClienteRepository;
import com.com4energy.processor.service.measure.MeasureRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@Order(200)
@RequiredArgsConstructor
public class ClientExistsRecordValidator implements MeasureRecordValidator {

    private final ThreadLocal<Map<String, Optional<String>>> batchCache = new ThreadLocal<>();

    private final ClienteRepository clienteRepository;

    @Override
    public String brokenRule() {
        return "CLIENT_EXISTS";
    }

    @Override
    public boolean supports(MeasureRecord measureRecord) {
        return true;
    }

    @Override
    public void beforeBatch(List<MeasureRecord> measureRecords) {
        batchCache.set(new HashMap<>());
    }

    @Override
    public void afterBatch() {
        batchCache.remove();
    }

    @Override
    public Optional<String> validate(MeasureRecord measureRecords) {
        String cups = measureRecords.cups();
        // Estas incidencias ya las cubren MandatoryFields y CupsFormat.
        if (cups == null || cups.isBlank()) {
            return Optional.empty();
        }

        String normalizedCups = cups.trim();

        Map<String, Optional<String>> cache = batchCache.get();
        if (cache == null) {
            return resolveValidation(normalizedCups, cups);
        }

        return cache.computeIfAbsent(normalizedCups, key -> resolveValidation(key, cups));
    }

    private Optional<String> resolveValidation(String normalizedCups, String originalCups) {
        List<ClienteRepository.ClienteLookupView> matches = clienteRepository.findLookupByCups(
                normalizedCups,
                PageRequest.of(0, 2)
        );
        if (matches.isEmpty()) {
            return Optional.of("No se encontró cliente para CUPS " + originalCups);
        }
        if (matches.size() > 1) {
            return Optional.of("Se encontró más de un cliente para CUPS " + originalCups);
        }

        ClienteRepository.ClienteLookupView client = matches.getFirst();
        if (client.getId() == null) {
            return Optional.of("El cliente para CUPS " + originalCups + " no tiene id_cliente");
        }

        return Optional.empty();
    }
}
