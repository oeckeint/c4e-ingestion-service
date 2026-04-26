package com.com4energy.processor.service.measure.validation;

import com.com4energy.processor.repository.ClienteRepository;
import com.com4energy.processor.service.measure.MeasureRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@Order(200)
@RequiredArgsConstructor
public class ClientExistsRecordValidator implements MeasureRecordValidator {

    private final ClienteRepository clienteRepository;

    @Override
    public String brokenRule() {
        return "CLIENT_EXISTS";
    }

    @Override
    public boolean supports(MeasureRecord record) {
        return true;
    }

    @Override
    public Optional<String> validate(MeasureRecord record) {
        String cups = record.cups();
        // Estas incidencias ya las cubren MandatoryFields y CupsFormat.
        if (cups == null || cups.isBlank()) {
            return Optional.empty();
        }

        String normalizedCups = cups.trim();

        List<ClienteRepository.ClienteLookupView> matches = clienteRepository.findLookupByCups(
                normalizedCups,
                PageRequest.of(0, 2)
        );
        if (matches.isEmpty()) {
            return Optional.of("No se encontró cliente para CUPS " + cups);
        }
        if (matches.size() > 1) {
            return Optional.of("Se encontró más de un cliente para CUPS " + cups);
        }

        ClienteRepository.ClienteLookupView client = matches.get(0);
        if (client.getId() == null) {
            return Optional.of("El cliente para CUPS " + cups + " no tiene id_cliente");
        }

        return Optional.empty();
    }
}
