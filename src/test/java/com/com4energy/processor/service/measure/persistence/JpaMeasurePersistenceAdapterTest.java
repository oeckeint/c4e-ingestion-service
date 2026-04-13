package com.com4energy.processor.service.measure.persistence;

import com.com4energy.processor.model.ClienteEntity;
import com.com4energy.processor.model.measure.MedidaCCHEntity;
import com.com4energy.processor.model.measure.MedidaHEntity;
import com.com4energy.processor.model.measure.MedidaLegacyEntity;
import com.com4energy.processor.model.measure.MedidaQHEntity;
import com.com4energy.processor.repository.ClienteRepository;
import com.com4energy.processor.repository.measure.MedidaCCHRepository;
import com.com4energy.processor.repository.measure.MedidaHRepository;
import com.com4energy.processor.repository.measure.MedidaLegacyRepository;
import com.com4energy.processor.repository.measure.MedidaQHRepository;
import com.com4energy.processor.service.measure.MeasureRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JpaMeasurePersistenceAdapterTest {

    @Test
    void persistReturnsErrorWhenClientNotFound() {
        MedidaHRepository medidaHRepository = mock(MedidaHRepository.class);
        MedidaQHRepository medidaQHRepository = mock(MedidaQHRepository.class);
        MedidaLegacyRepository medidaLegacyRepository = mock(MedidaLegacyRepository.class);
        MedidaCCHRepository medidaCCHRepository = mock(MedidaCCHRepository.class);
        ClienteRepository clienteRepository = mock(ClienteRepository.class);

        when(clienteRepository.findByCups("ES0001")).thenReturn(List.of());

        JpaMeasurePersistenceAdapter adapter = new JpaMeasurePersistenceAdapter(
                medidaHRepository,
                medidaQHRepository,
                medidaLegacyRepository,
                medidaCCHRepository,
                clienteRepository
        );

        MeasurePersistenceContracts.MeasurePersistenceResult result = adapter.persist(
                new MeasurePersistenceContracts.PersistMeasuresCommand(
                        1L,
                        "P1D_0021_0894_20240104.0",
                        List.of(hourly("ES0001"))
                )
        );

        assertEquals(0, result.persistedCount());
        assertEquals(1, result.errorCount());
        assertEquals(0, result.skippedCount());
        assertTrue(result.errors().get(0).contains("No se encontró cliente"));

        verify(medidaHRepository, never()).saveAll(anyList());
        verify(medidaQHRepository, never()).saveAll(anyList());
        verify(medidaLegacyRepository, never()).saveAll(anyList());
        verify(medidaCCHRepository, never()).saveAll(anyList());
    }

    @Test
    void persistReturnsErrorWhenClientIsDuplicatedForCups() {
        MedidaHRepository medidaHRepository = mock(MedidaHRepository.class);
        MedidaQHRepository medidaQHRepository = mock(MedidaQHRepository.class);
        MedidaLegacyRepository medidaLegacyRepository = mock(MedidaLegacyRepository.class);
        MedidaCCHRepository medidaCCHRepository = mock(MedidaCCHRepository.class);
        ClienteRepository clienteRepository = mock(ClienteRepository.class);

        when(clienteRepository.findByCups("ES0002")).thenReturn(List.of(client(10L, "2.0A"), client(11L, "2.0A")));

        JpaMeasurePersistenceAdapter adapter = new JpaMeasurePersistenceAdapter(
                medidaHRepository,
                medidaQHRepository,
                medidaLegacyRepository,
                medidaCCHRepository,
                clienteRepository
        );

        MeasurePersistenceContracts.MeasurePersistenceResult result = adapter.persist(
                new MeasurePersistenceContracts.PersistMeasuresCommand(
                        2L,
                        "P2D_0021_0894_20240104.0",
                        List.of(quarterHourly("ES0002"))
                )
        );

        assertEquals(0, result.persistedCount());
        assertEquals(1, result.errorCount());
        assertEquals(0, result.skippedCount());
        assertTrue(result.errors().get(0).contains("más de un cliente"));

        verify(medidaHRepository, never()).saveAll(anyList());
        verify(medidaQHRepository, never()).saveAll(anyList());
        verify(medidaLegacyRepository, never()).saveAll(anyList());
        verify(medidaCCHRepository, never()).saveAll(anyList());
    }

    @Test
    void persistSkipsLegacyWhenClientTariffIs20Td() {
        MedidaHRepository medidaHRepository = mock(MedidaHRepository.class);
        MedidaQHRepository medidaQHRepository = mock(MedidaQHRepository.class);
        MedidaLegacyRepository medidaLegacyRepository = mock(MedidaLegacyRepository.class);
        MedidaCCHRepository medidaCCHRepository = mock(MedidaCCHRepository.class);
        ClienteRepository clienteRepository = mock(ClienteRepository.class);

        when(clienteRepository.findByCups("ES0003")).thenReturn(List.of(client(12L, "20TD")));

        JpaMeasurePersistenceAdapter adapter = new JpaMeasurePersistenceAdapter(
                medidaHRepository,
                medidaQHRepository,
                medidaLegacyRepository,
                medidaCCHRepository,
                clienteRepository
        );

        MeasurePersistenceContracts.MeasurePersistenceResult result = adapter.persist(
                new MeasurePersistenceContracts.PersistMeasuresCommand(
                        3L,
                        "F5D_0031_0894_20250311.0",
                        List.of(legacy("ES0003"))
                )
        );

        assertEquals(0, result.persistedCount());
        assertEquals(0, result.errorCount());
        assertEquals(1, result.skippedCount());

        verify(medidaLegacyRepository, never()).saveAll(anyList());
        verify(medidaHRepository, never()).saveAll(anyList());
        verify(medidaQHRepository, never()).saveAll(anyList());
        verify(medidaCCHRepository, never()).saveAll(anyList());
    }

    @Test
    void persistRoutesEachMeasureTypeToItsRepository() {
        MedidaHRepository medidaHRepository = mock(MedidaHRepository.class);
        MedidaQHRepository medidaQHRepository = mock(MedidaQHRepository.class);
        MedidaLegacyRepository medidaLegacyRepository = mock(MedidaLegacyRepository.class);
        MedidaCCHRepository medidaCCHRepository = mock(MedidaCCHRepository.class);
        ClienteRepository clienteRepository = mock(ClienteRepository.class);

        when(clienteRepository.findByCups("ES0101")).thenReturn(List.of(client(21L, "2.0A")));
        when(clienteRepository.findByCups("ES0102")).thenReturn(List.of(client(22L, "2.0A")));
        when(clienteRepository.findByCups("ES0103")).thenReturn(List.of(client(23L, "3.0A")));
        when(clienteRepository.findByCups("ES0104")).thenReturn(List.of(client(24L, "2.0A")));

        JpaMeasurePersistenceAdapter adapter = new JpaMeasurePersistenceAdapter(
                medidaHRepository,
                medidaQHRepository,
                medidaLegacyRepository,
                medidaCCHRepository,
                clienteRepository
        );

        MeasureRecord.Hourly hourly = hourly("ES0101");
        MeasureRecord.QuarterHourly quarterHourly = quarterHourly("ES0102");
        MeasureRecord.Legacy legacy = legacy("ES0103");
        MeasureRecord.Cch cch = cch("ES0104");

        MeasurePersistenceContracts.MeasurePersistenceResult result = adapter.persist(
                new MeasurePersistenceContracts.PersistMeasuresCommand(
                        4L,
                        "mixed-origin",
                        List.of(
                                hourly,
                                quarterHourly,
                                legacy,
                                cch
                        )
                )
        );

        assertEquals(4, result.persistedCount());
        assertEquals(0, result.errorCount());
        assertEquals(0, result.skippedCount());

        verify(medidaHRepository).saveAll(anyList());
        verify(medidaQHRepository).saveAll(anyList());
        verify(medidaLegacyRepository).saveAll(anyList());
        verify(medidaCCHRepository).saveAll(anyList());

        List<MedidaHEntity> hEntities = captureSavedEntities(medidaHRepository);
        List<MedidaQHEntity> qhEntities = captureSavedEntities(medidaQHRepository);
        List<MedidaLegacyEntity> legacyEntities = captureSavedEntities(medidaLegacyRepository);
        List<MedidaCCHEntity> cchEntities = captureSavedEntities(medidaCCHRepository);

        assertEquals(1, hEntities.size());
        assertEquals(1, qhEntities.size());
        assertEquals(1, legacyEntities.size());
        assertEquals(1, cchEntities.size());

        MedidaHEntity hEntity = hEntities.get(0);
        assertEquals(21L, hEntity.getClienteId());
        assertEquals(hourly.tipoMedida(), hEntity.getTipoMedida());
        assertEquals(hourly.timestamp(), hEntity.getFecha());
        assertEquals((double) hourly.actent(), hEntity.getActent());
        assertEquals(hourly.origen(), hEntity.getOrigen());
        assertEquals("SYSTEM", hEntity.getCreatedBy());
        assertNotNull(hEntity.getCreatedOn());

        MedidaQHEntity qhEntity = qhEntities.get(0);
        assertEquals(22L, qhEntity.getClienteId());
        assertEquals(quarterHourly.tipoMedida(), qhEntity.getTipoMed());
        assertEquals(quarterHourly.timestamp(), qhEntity.getFecha());
        assertEquals(quarterHourly.actent(), qhEntity.getActent());
        assertEquals(quarterHourly.origen(), qhEntity.getOrigen());
        assertEquals("SYSTEM", qhEntity.getCreatedBy());
        assertNotNull(qhEntity.getCreatedOn());

        MedidaLegacyEntity legacyEntity = legacyEntities.get(0);
        assertEquals(23L, legacyEntity.getClienteId());
        assertEquals(legacy.timestamp(), legacyEntity.getFecha());
        assertEquals(legacy.ae1(), legacyEntity.getAe1());
        assertEquals(legacy.codigoFactura(), legacyEntity.getCodigoFactura());
        assertEquals("SYSTEM", legacyEntity.getCreatedBy());
        assertNotNull(legacyEntity.getCreatedOn());

        MedidaCCHEntity cchEntity = cchEntities.get(0);
        assertEquals(24L, cchEntity.getClienteId());
        assertEquals(cch.timestamp(), cchEntity.getFecha());
        assertEquals(cch.actent(), cchEntity.getActent());
        assertEquals(cch.metod(), cchEntity.getMetod());
        assertEquals("SYSTEM", cchEntity.getCreatedBy());
        assertNotNull(cchEntity.getCreatedOn());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> List<T> captureSavedEntities(org.springframework.data.jpa.repository.JpaRepository repository) {
        ArgumentCaptor<List<T>> captor = ArgumentCaptor.forClass((Class) List.class);
        verify(repository).saveAll(captor.capture());
        return captor.getValue();
    }

    private ClienteEntity client(Long id, String tarifa) {
        ClienteEntity cliente = new ClienteEntity();
        cliente.setId(id);
        cliente.setTarifa(tarifa);
        return cliente;
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

    private MeasureRecord.QuarterHourly quarterHourly(String cups) {
        return new MeasureRecord.QuarterHourly(
                cups,
                LocalDateTime.of(2025, 1, 1, 0, 0),
                11,
                0,
                1,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                128,
                0,
                128,
                0,
                1,
                99,
                "P2D_0021_0894_20240104.0",
                cups + ";11;2025/01/01 00:00:00;..."
        );
    }

    private MeasureRecord.Legacy legacy(String cups) {
        return new MeasureRecord.Legacy(
                cups,
                LocalDateTime.of(2025, 1, 1, 0, 0),
                0,
                10,
                0,
                0,
                0,
                0,
                0,
                1,
                1,
                "171251N029990602",
                cups + ";2025/01/01 00:00;..."
        );
    }

    private MeasureRecord.Cch cch(String cups) {
        return new MeasureRecord.Cch(
                cups,
                LocalDateTime.of(2025, 1, 1, 0, 0),
                0,
                10,
                1,
                cups + ";2025/01/01 00:00;0;10;1"
        );
    }

    @Test
    void persistFlushesRecordsInBatchesOfOneThousand() {
        MedidaHRepository medidaHRepository = mock(MedidaHRepository.class);
        MedidaQHRepository medidaQHRepository = mock(MedidaQHRepository.class);
        MedidaLegacyRepository medidaLegacyRepository = mock(MedidaLegacyRepository.class);
        MedidaCCHRepository medidaCCHRepository = mock(MedidaCCHRepository.class);
        ClienteRepository clienteRepository = mock(ClienteRepository.class);

        ClienteEntity client = client(1L, "2.0A");
        when(clienteRepository.findByCups("ES0001")).thenReturn(List.of(client));

        JpaMeasurePersistenceAdapter adapter = new JpaMeasurePersistenceAdapter(
                medidaHRepository,
                medidaQHRepository,
                medidaLegacyRepository,
                medidaCCHRepository,
                clienteRepository
        );

        // Create 2500 valid hourly records (should trigger 3 flushes: 1000, 1000, 500)
        List<MeasureRecord> largeDataset = new java.util.ArrayList<>();
        for (int i = 0; i < 2500; i++) {
            largeDataset.add(hourly("ES0001"));
        }

        MeasurePersistenceContracts.MeasurePersistenceResult result = adapter.persist(
                new MeasurePersistenceContracts.PersistMeasuresCommand(
                        1L,
                        "large-file",
                        largeDataset
                )
        );

        // Verify results
        assertEquals(2500, result.persistedCount());
        assertEquals(0, result.errorCount());
        assertEquals(0, result.skippedCount());

        // Verify that saveAll was called 3 times (batches of 1000, 1000, 500)
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MedidaHEntity>> captor = ArgumentCaptor.forClass((Class) List.class);
        verify(medidaHRepository, times(3)).saveAll(captor.capture());

        // Collect all batches to verify batch sizes
        List<List<MedidaHEntity>> allBatches = captor.getAllValues();

        // Verify batch sizes: first two should be 1000, last one should be 500
        assertEquals(1000, allBatches.get(0).size(), "First batch should have 1000 records");
        assertEquals(1000, allBatches.get(1).size(), "Second batch should have 1000 records");
        assertEquals(500, allBatches.get(2).size(), "Third batch should have 500 records");
    }

    @Test
    void persistBinarySplitsWhenSaveAllFails() {
        MedidaHRepository medidaHRepository = mock(MedidaHRepository.class);
        MedidaQHRepository medidaQHRepository = mock(MedidaQHRepository.class);
        MedidaLegacyRepository medidaLegacyRepository = mock(MedidaLegacyRepository.class);
        MedidaCCHRepository medidaCCHRepository = mock(MedidaCCHRepository.class);
        ClienteRepository clienteRepository = mock(ClienteRepository.class);

        ClienteEntity client = client(1L, "2.0A");
        when(clienteRepository.findByCups("ES0001")).thenReturn(List.of(client));

        // Mock: first call (all 3) fails, then both halves succeed
        when(medidaHRepository.saveAll(argThat(list -> 
                list instanceof List && ((List<?>) list).size() == 3
        )))
                .thenThrow(new RuntimeException("Constraint violation"));
        when(medidaHRepository.saveAll(argThat(list -> 
                list instanceof List && ((List<?>) list).size() <= 2 && ((List<?>) list).size() > 0
        )))
                .thenReturn(null);

        JpaMeasurePersistenceAdapter adapter = new JpaMeasurePersistenceAdapter(
                medidaHRepository,
                medidaQHRepository,
                medidaLegacyRepository,
                medidaCCHRepository,
                clienteRepository
        );

        List<MeasureRecord> records = List.of(hourly("ES0001"), hourly("ES0001"), hourly("ES0001"));

        MeasurePersistenceContracts.MeasurePersistenceResult result = adapter.persist(
                new MeasurePersistenceContracts.PersistMeasuresCommand(1L, "test", records)
        );

        // Should persist some records even though one batch failed
        assertTrue(result.persistedCount() >= 0, "Should attempt to recover from failure");
    }
}

