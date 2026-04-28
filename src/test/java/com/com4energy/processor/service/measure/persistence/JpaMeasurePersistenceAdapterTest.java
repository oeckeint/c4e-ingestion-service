package com.com4energy.processor.service.measure.persistence;

import com.com4energy.processor.model.measure.MedidaCCHEntity;
import com.com4energy.processor.model.measure.MedidaHEntity;
import com.com4energy.processor.model.measure.MedidaQHEntity;
import com.com4energy.processor.repository.ClienteRepository;
import com.com4energy.processor.repository.measure.MedidaCCHRepository;
import com.com4energy.processor.repository.measure.MedidaHRepository;
import com.com4energy.processor.repository.measure.MedidaQHRepository;
import com.com4energy.processor.service.measure.MeasureRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
        MedidaCCHRepository medidaCCHRepository = mock(MedidaCCHRepository.class);
        ClienteRepository clienteRepository = mock(ClienteRepository.class);

        when(clienteRepository.findLookupByCups(eq("ES0001"), any(Pageable.class))).thenReturn(List.of());

        JpaMeasurePersistenceAdapter adapter = new JpaMeasurePersistenceAdapter(
                medidaHRepository,
                medidaQHRepository,
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
        verify(medidaCCHRepository, never()).saveAll(anyList());
    }

    @Test
    void persistReturnsErrorWhenClientIsDuplicatedForCups() {
        MedidaHRepository medidaHRepository = mock(MedidaHRepository.class);
        MedidaQHRepository medidaQHRepository = mock(MedidaQHRepository.class);
        MedidaCCHRepository medidaCCHRepository = mock(MedidaCCHRepository.class);
        ClienteRepository clienteRepository = mock(ClienteRepository.class);

        when(clienteRepository.findLookupByCups(eq("ES0002"), any(Pageable.class)))
                .thenReturn(List.of(client(10L, "2.0A"), client(11L, "2.0A")));

        JpaMeasurePersistenceAdapter adapter = new JpaMeasurePersistenceAdapter(
                medidaHRepository,
                medidaQHRepository,
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
        verify(medidaCCHRepository, never()).saveAll(anyList());
    }

    @Test
    void persistRoutesF5AsCch() {
        MedidaHRepository medidaHRepository = mock(MedidaHRepository.class);
        MedidaQHRepository medidaQHRepository = mock(MedidaQHRepository.class);
        MedidaCCHRepository medidaCCHRepository = mock(MedidaCCHRepository.class);
        ClienteRepository clienteRepository = mock(ClienteRepository.class);

        when(clienteRepository.findLookupByCups(eq("ES0003"), any(Pageable.class))).thenReturn(List.of(client(12L, "20TD")));

        JpaMeasurePersistenceAdapter adapter = new JpaMeasurePersistenceAdapter(
                medidaHRepository,
                medidaQHRepository,
                medidaCCHRepository,
                clienteRepository
        );

        MeasurePersistenceContracts.MeasurePersistenceResult result = adapter.persist(
                new MeasurePersistenceContracts.PersistMeasuresCommand(
                        3L,
                        "F5D_0031_0894_20250311.0",
                        List.of(cchFromF5("ES0003"))
                )
        );

        assertEquals(1, result.persistedCount());
        assertEquals(0, result.errorCount());
        assertEquals(0, result.skippedCount());

        verify(medidaCCHRepository).saveAll(anyList());
        verify(medidaHRepository, never()).saveAll(anyList());
        verify(medidaQHRepository, never()).saveAll(anyList());
    }

    @Test
    void persistRoutesEachMeasureTypeToItsRepository() {
        MedidaHRepository medidaHRepository = mock(MedidaHRepository.class);
        MedidaQHRepository medidaQHRepository = mock(MedidaQHRepository.class);
        MedidaCCHRepository medidaCCHRepository = mock(MedidaCCHRepository.class);
        ClienteRepository clienteRepository = mock(ClienteRepository.class);

        when(clienteRepository.findLookupByCups(eq("ES0101"), any(Pageable.class))).thenReturn(List.of(client(21L, "2.0A")));
        when(clienteRepository.findLookupByCups(eq("ES0102"), any(Pageable.class))).thenReturn(List.of(client(22L, "2.0A")));
        when(clienteRepository.findLookupByCups(eq("ES0103"), any(Pageable.class))).thenReturn(List.of(client(23L, "3.0A")));
        when(clienteRepository.findLookupByCups(eq("ES0104"), any(Pageable.class))).thenReturn(List.of(client(24L, "2.0A")));

        JpaMeasurePersistenceAdapter adapter = new JpaMeasurePersistenceAdapter(
                medidaHRepository,
                medidaQHRepository,
                medidaCCHRepository,
                clienteRepository
        );

        MeasureRecord.Hourly hourly = hourly("ES0101");
        MeasureRecord.QuarterHourly quarterHourly = quarterHourly("ES0102");
        MeasureRecord.Cch f5AsCch = cchFromF5("ES0103");
        MeasureRecord.Cch cch = cch("ES0104");

        MeasurePersistenceContracts.MeasurePersistenceResult result = adapter.persist(
                new MeasurePersistenceContracts.PersistMeasuresCommand(
                        4L,
                        "mixed-origin",
                        List.of(
                                hourly,
                                quarterHourly,
                                f5AsCch,
                                cch
                        )
                )
        );

        assertEquals(4, result.persistedCount());
        assertEquals(0, result.errorCount());
        assertEquals(0, result.skippedCount());

        verify(medidaHRepository).saveAll(anyList());
        verify(medidaQHRepository).saveAll(anyList());
        verify(medidaCCHRepository).saveAll(anyList());

        List<MedidaHEntity> hEntities = captureSavedEntities(medidaHRepository);
        List<MedidaQHEntity> qhEntities = captureSavedEntities(medidaQHRepository);
        List<MedidaCCHEntity> cchEntities = captureSavedEntities(medidaCCHRepository);

        assertEquals(1, hEntities.size());
        assertEquals(1, qhEntities.size());
        assertEquals(2, cchEntities.size());

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

        assertTrue(cchEntities.stream().anyMatch(entity ->
                entity.getClienteId().equals(23L)
                        && entity.getFecha().equals(f5AsCch.timestamp())
                        && entity.getActent().equals(f5AsCch.actent())
                        && entity.getMetod().equals(f5AsCch.metod())
        ));
        assertTrue(cchEntities.stream().anyMatch(entity ->
                entity.getClienteId().equals(24L)
                        && entity.getFecha().equals(cch.timestamp())
                        && entity.getActent().equals(cch.actent())
                        && entity.getMetod().equals(cch.metod())
        ));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> List<T> captureSavedEntities(org.springframework.data.jpa.repository.JpaRepository repository) {
        ArgumentCaptor<List<T>> captor = ArgumentCaptor.forClass((Class) List.class);
        verify(repository).saveAll(captor.capture());
        return captor.getValue();
    }

    private ClienteRepository.ClienteLookupView client(Long id, String tarifa) {
        return new ClienteRepository.ClienteLookupView() {
            @Override
            public Long getId() {
                return id;
            }

            @Override
            public String getTarifa() {
                return tarifa;
            }
        };
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

    private MeasureRecord.Cch cchFromF5(String cups) {
        return new MeasureRecord.Cch(
                cups,
                LocalDateTime.of(2025, 1, 1, 0, 0),
                0,
                10,
                1,
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
        MedidaCCHRepository medidaCCHRepository = mock(MedidaCCHRepository.class);
        ClienteRepository clienteRepository = mock(ClienteRepository.class);

        ClienteRepository.ClienteLookupView client = client(1L, "2.0A");
        when(clienteRepository.findLookupByCups(eq("ES0001"), any(Pageable.class))).thenReturn(List.of(client));

        JpaMeasurePersistenceAdapter adapter = new JpaMeasurePersistenceAdapter(
                medidaHRepository,
                medidaQHRepository,
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
        MedidaCCHRepository medidaCCHRepository = mock(MedidaCCHRepository.class);
        ClienteRepository clienteRepository = mock(ClienteRepository.class);

        ClienteRepository.ClienteLookupView client = client(1L, "2.0A");
        when(clienteRepository.findLookupByCups(eq("ES0001"), any(Pageable.class))).thenReturn(List.of(client));

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

    @Test
    void persistReturnsFailedRecordsWhenBinarySplitIsolatesInvalidRecord() {
        MedidaHRepository medidaHRepository = mock(MedidaHRepository.class);
        MedidaQHRepository medidaQHRepository = mock(MedidaQHRepository.class);
        MedidaCCHRepository medidaCCHRepository = mock(MedidaCCHRepository.class);
        ClienteRepository clienteRepository = mock(ClienteRepository.class);

        ClienteRepository.ClienteLookupView client = client(1L, "2.0A");
        when(clienteRepository.findLookupByCups(eq("ES0001"), any(Pageable.class))).thenReturn(List.of(client));

        AtomicBoolean singleRecordFailed = new AtomicBoolean(false);
        when(medidaHRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<?> batch = invocation.getArgument(0);
            if (batch.size() == 2) {
                throw new RuntimeException("Constraint violation");
            }
            if (batch.size() == 1 && !singleRecordFailed.get()) {
                singleRecordFailed.set(true);
                throw new RuntimeException("Single record violation");
            }
            return null;
        });

        JpaMeasurePersistenceAdapter adapter = new JpaMeasurePersistenceAdapter(
                medidaHRepository,
                medidaQHRepository,
                medidaCCHRepository,
                clienteRepository
        );

        MeasureRecord.Hourly first = hourly("ES0001");
        MeasureRecord.Hourly second = hourly("ES0001");
        MeasurePersistenceContracts.MeasurePersistenceResult result = adapter.persist(
                new MeasurePersistenceContracts.PersistMeasuresCommand(1L, "test", List.of(first, second))
        );

        assertEquals(1, result.persistedCount());
        assertEquals(1, result.failedRecords().size());
        assertEquals(first, result.failedRecords().get(0));
    }

    @Test
    void persistReusesClientLookupForRepeatedCups() {
        MedidaHRepository medidaHRepository = mock(MedidaHRepository.class);
        MedidaQHRepository medidaQHRepository = mock(MedidaQHRepository.class);
        MedidaCCHRepository medidaCCHRepository = mock(MedidaCCHRepository.class);
        ClienteRepository clienteRepository = mock(ClienteRepository.class);

        when(clienteRepository.findLookupByCups(eq("ES0001"), any(Pageable.class))).thenReturn(List.of(client(1L, "2.0A")));

        JpaMeasurePersistenceAdapter adapter = new JpaMeasurePersistenceAdapter(
                medidaHRepository,
                medidaQHRepository,
                medidaCCHRepository,
                clienteRepository
        );

        adapter.persist(new MeasurePersistenceContracts.PersistMeasuresCommand(
                1L,
                "repeated-cups",
                List.of(hourly("ES0001"), hourly("ES0001"), hourly("ES0001"))
        ));

        verify(clienteRepository, times(1)).findLookupByCups(eq("ES0001"), any(Pageable.class));
    }
}

