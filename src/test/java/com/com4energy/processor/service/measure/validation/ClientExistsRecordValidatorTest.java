package com.com4energy.processor.service.measure.validation;

import com.com4energy.processor.model.ClienteEntity;
import com.com4energy.processor.repository.ClienteRepository;
import com.com4energy.processor.service.measure.MeasureRecord;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClientExistsRecordValidatorTest {

    @Test
    void returnsErrorWhenClientDoesNotExist() {
        ClienteRepository clienteRepository = mock(ClienteRepository.class);
        ClientExistsRecordValidator validator = new ClientExistsRecordValidator(clienteRepository);

        when(clienteRepository.findByCups("ES123456789012345678")).thenReturn(List.of());

        var result = validator.validate(hourly("ES123456789012345678"));

        assertTrue(result.isPresent());
        assertTrue(result.get().contains("No se encontró cliente"));
    }

    @Test
    void returnsErrorWhenMoreThanOneClientFound() {
        ClienteRepository clienteRepository = mock(ClienteRepository.class);
        ClientExistsRecordValidator validator = new ClientExistsRecordValidator(clienteRepository);

        ClienteEntity c1 = new ClienteEntity();
        c1.setId(1L);
        ClienteEntity c2 = new ClienteEntity();
        c2.setId(2L);

        when(clienteRepository.findByCups("ES123456789012345678")).thenReturn(List.of(c1, c2));

        var result = validator.validate(hourly("ES123456789012345678"));

        assertTrue(result.isPresent());
        assertTrue(result.get().contains("más de un cliente"));
    }

    @Test
    void returnsErrorWhenClientHasNullId() {
        ClienteRepository clienteRepository = mock(ClienteRepository.class);
        ClientExistsRecordValidator validator = new ClientExistsRecordValidator(clienteRepository);

        ClienteEntity c1 = new ClienteEntity();
        c1.setId(null);

        when(clienteRepository.findByCups("ES123456789012345678")).thenReturn(List.of(c1));

        var result = validator.validate(hourly("ES123456789012345678"));

        assertTrue(result.isPresent());
        assertTrue(result.get().contains("no tiene id_cliente"));
    }

    @Test
    void returnsEmptyWhenClientExistsWithId() {
        ClienteRepository clienteRepository = mock(ClienteRepository.class);
        ClientExistsRecordValidator validator = new ClientExistsRecordValidator(clienteRepository);

        ClienteEntity c1 = new ClienteEntity();
        c1.setId(1L);

        when(clienteRepository.findByCups("ES123456789012345678")).thenReturn(List.of(c1));

        var result = validator.validate(hourly("ES123456789012345678"));

        assertTrue(result.isEmpty());
        assertEquals("CLIENT_EXISTS", validator.brokenRule());
    }

    private MeasureRecord.Hourly hourly(String cups) {
        return new MeasureRecord.Hourly(
                cups,
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
                cups + ";11;2025/01/01 00:00:00;..."
        );
    }
}
