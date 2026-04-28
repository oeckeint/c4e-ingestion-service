package com.com4energy.processor.service.measure.persistence;

import com.com4energy.processor.model.measure.MedidaCCHEntity;
import com.com4energy.processor.model.measure.MedidaHEntity;
import com.com4energy.processor.model.measure.MedidaQHEntity;
import com.com4energy.processor.repository.ClienteRepository;
import com.com4energy.processor.repository.measure.MedidaCCHRepository;
import com.com4energy.processor.repository.measure.MedidaHRepository;
import com.com4energy.processor.repository.measure.MedidaQHRepository;
import com.com4energy.processor.service.measure.MeasureRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JpaMeasurePersistenceAdapter implements MeasurePersistenceContracts.MeasurePersistencePort {

    private static final String SYSTEM_USER = "SYSTEM";
    private static final int DEFAULT_BATCH_SIZE = 1000;

    private final MedidaHRepository medidaHRepository;
    private final MedidaQHRepository medidaQHRepository;
    private final MedidaCCHRepository medidaCCHRepository;
    private final ClienteRepository clienteRepository;

    // Contexto para tracking durante binary split
    private final ThreadLocal<BatchSplitContext> batchContext = ThreadLocal.withInitial(BatchSplitContext::new);

    @Override
    @Transactional
    public MeasurePersistenceContracts.MeasurePersistenceResult persist(
            MeasurePersistenceContracts.PersistMeasuresCommand command
    ) {
        BatchSplitContext context = batchContext.get();
        context.reset();

        List<String> errors = new ArrayList<>();
        Map<String, ClienteResolution> clientCache = new HashMap<>();

        List<MedidaHEntity> p1Batch = new ArrayList<>();
        List<MedidaQHEntity> p2Batch = new ArrayList<>();
        List<MedidaCCHEntity> cchBatch = new ArrayList<>();

        int skipped = 0;
        int persisted = 0;

        try {
            for (MeasureRecord measureRecord : command.measureRecords()) {
                ClienteResolution resolution = resolveClient(measureRecord.cups(), clientCache);
                boolean hasInvalidClient = !resolution.valid();

                if (hasInvalidClient) {
                    errors.add(resolution.errorMessage());
                } else {
                    if (measureRecord instanceof MeasureRecord.Hourly hourly) {
                        MedidaHEntity entity = toMedidaH(hourly, resolution.clientId());
                        context.registerSource(entity, measureRecord);
                        p1Batch.add(entity);
                    } else if (measureRecord instanceof MeasureRecord.QuarterHourly quarterHourly) {
                        MedidaQHEntity entity = toMedidaQH(quarterHourly, resolution.clientId());
                        context.registerSource(entity, measureRecord);
                        p2Batch.add(entity);
                    } else if (measureRecord instanceof MeasureRecord.Cch cch) {
                        MedidaCCHEntity entity = toMedidaCch(cch, resolution.clientId());
                        context.registerSource(entity, measureRecord);
                        cchBatch.add(entity);
                    }

                    // Persist in batches to optimize performance while maintaining transactional integrity
                    if (p1Batch.size() + p2Batch.size() + cchBatch.size() >= DEFAULT_BATCH_SIZE) {
                        persisted += flushBatches(p1Batch, p2Batch, cchBatch);
                    }
                }
            }

            // Flush any remaining records
            persisted += flushBatches(p1Batch, p2Batch, cchBatch);

            return new MeasurePersistenceContracts.MeasurePersistenceResult(
                    persisted,
                    errors.size(),
                    skipped,
                    errors,
                    context.getFailedRecords(),
                    null
            );
        } finally {
            context.reset();
            batchContext.remove();
        }
    }

    /**
     * Persiste los lotes acumulados con binary split en caso de error.
     * Si saveAll falla, divide recursivamente el lote hasta aislar registros malos.
     * Acumula los registros que fallan en el contexto de batch.
     *
     * @return número de registros persistidos
     */
    private int flushBatches(
            List<MedidaHEntity> p1Batch,
            List<MedidaQHEntity> p2Batch,
            List<MedidaCCHEntity> cchBatch
    ) {
        int persistedCount = 0;

        if (!p1Batch.isEmpty()) {
            persistedCount += flushWithBinarySplit(p1Batch, medidaHRepository, EntityType.P1_H);
            p1Batch.clear();
        }
        if (!p2Batch.isEmpty()) {
            persistedCount += flushWithBinarySplit(p2Batch, medidaQHRepository, EntityType.P2_QH);
            p2Batch.clear();
        }
        if (!cchBatch.isEmpty()) {
            persistedCount += flushWithBinarySplit(cchBatch, medidaCCHRepository, EntityType.F5_CCH);
            cchBatch.clear();
        }

        return persistedCount;
    }

    /**
     * Flush con binary split: intenta persistir lote.
     * Si falla, divide recursivamente hasta aislar el registro problemático.
     */
    private <T> int flushWithBinarySplit(
            List<T> batch,
            org.springframework.data.jpa.repository.JpaRepository<T, ?> repository,
            EntityType entityType
    ) {
        if (batch.isEmpty()) {
            return 0;
        }

        try {
            repository.saveAll(new ArrayList<>(batch));
            return batch.size();
        } catch (Exception e) {
            log.warn(
                    "Batch flush failed for {} records of type {}. Attempting binary split. Error: {}",
                    batch.size(),
                    entityType,
                    e.getMessage()
            );
            return binarySplitAndPersist(batch, repository, entityType, e.getMessage());
        }
    }

    /**
     * Binary split recursivo: divide lote por mitad y reintenta.
     * Acumula registros que fallan en el contexto para cuarentena.
     */
    private <T> int binarySplitAndPersist(
            List<T> batch,
            org.springframework.data.jpa.repository.JpaRepository<T, ?> repository,
            EntityType entityType,
            String errorMessage
    ) {
        if (batch.isEmpty()) {
            return 0;
        }

        if (batch.size() == 1) {
            // Base case: 1 registro = es el malo
            logFailedRecord(entityType, errorMessage);
            batchContext.get().addFailedEntity(batch.get(0));
            return 0;
        }

        int mid = batch.size() / 2;
        List<T> first = new ArrayList<>(batch.subList(0, mid));
        List<T> second = new ArrayList<>(batch.subList(mid, batch.size()));

        int persistedCount = 0;

        // Reintenta primera mitad
        try {
            repository.saveAll(first);
            persistedCount += first.size();
            log.debug("First half ({} records) persisted successfully", first.size());
        } catch (Exception e) {
            log.debug("First half failed, recursing with {} records", first.size());
            persistedCount += binarySplitAndPersist(first, repository, entityType, e.getMessage());
        }

        // Reintenta segunda mitad
        try {
            repository.saveAll(second);
            persistedCount += second.size();
            log.debug("Second half ({} records) persisted successfully", second.size());
        } catch (Exception e) {
            log.debug("Second half failed, recursing with {} records", second.size());
            persistedCount += binarySplitAndPersist(second, repository, entityType, e.getMessage());
        }

        return persistedCount;
    }

    private void logFailedRecord(EntityType entityType, String errorMessage) {
        log.warn(
                "Failed to persist record of type {}. Error: {}",
                entityType,
                errorMessage
        );
    }

    enum EntityType {
        P1_H, P2_QH, F5_CCH
    }

    /**
     * Contexto para tracking de registros fallidos durante binary split.
     */
    static class BatchSplitContext {
        private final List<MeasureRecord> failedRecords = new ArrayList<>();
        private final Map<Object, MeasureRecord> sourceRecords = new IdentityHashMap<>();

        void registerSource(Object entity, MeasureRecord sourceRecord) {
            sourceRecords.put(entity, sourceRecord);
        }

        void addFailedEntity(Object entity) {
            MeasureRecord sourceRecord = sourceRecords.get(entity);
            if (sourceRecord != null) {
                failedRecords.add(sourceRecord);
            }
        }

        List<MeasureRecord> getFailedRecords() {
            return new ArrayList<>(failedRecords);
        }

        void reset() {
            failedRecords.clear();
            sourceRecords.clear();
        }
    }

    private ClienteResolution resolveClient(String cups, Map<String, ClienteResolution> clientCache) {
        if (cups == null || cups.isBlank()) {
            return ClienteResolution.error("No se puede resolver el cliente porque el CUPS está vacío");
        }

        String normalizedCups = cups.trim();

        return clientCache.computeIfAbsent(normalizedCups, key -> {
            List<ClienteRepository.ClienteLookupView> matches = clienteRepository.findLookupByCups(
                    key,
                    PageRequest.of(0, 2)
            );
            if (matches.isEmpty()) {
                return ClienteResolution.error("No se encontró cliente para CUPS " + key);
            }
            if (matches.size() > 1) {
                return ClienteResolution.error("Se encontró más de un cliente para CUPS " + key);
            }

            ClienteRepository.ClienteLookupView client = matches.get(0);
            if (client.getId() == null) {
                return ClienteResolution.error("El cliente para CUPS " + key + " no tiene id_cliente");
            }
            return ClienteResolution.success(client.getId(), client.getTarifa());
        });
    }

    private MedidaHEntity toMedidaH(MeasureRecord.Hourly measure, Long clientId) {
        LocalDateTime now = LocalDateTime.now();
        return MedidaHEntity.builder()
                .clienteId(clientId)
                .tipoMedida(measure.tipoMedida())
                .fecha(measure.timestamp())
                .banderaInvVer((double) measure.banderaInvVer())
                .actent((double) measure.actent())
                .qactent((double) measure.qactent())
                .actsal((double) measure.actsal())
                .qactsal((double) measure.qactsal())
                .rq1((double) measure.rQ1())
                .qrq1((double) measure.qrQ1())
                .rq2((double) measure.rQ2())
                .qrq2((double) measure.qrQ2())
                .rq3((double) measure.rQ3())
                .qrq3((double) measure.qrQ3())
                .rq4((double) measure.rQ4())
                .qrq4((double) measure.qrQ4())
                .medres1((double) measure.medres1())
                .qmedres1((double) measure.qmedres1())
                .medres2((double) measure.medres2())
                .qmedres2((double) measure.qmedres2())
                .metodObt(measure.metodObt())
                .temporal(measure.temporal())
                .origen(measure.origen())
                .createdOn(now)
                .createdBy(SYSTEM_USER)
                .build();
    }

    private MedidaQHEntity toMedidaQH(MeasureRecord.QuarterHourly measure, Long clientId) {
        LocalDateTime now = LocalDateTime.now();
        return MedidaQHEntity.builder()
                .clienteId(clientId)
                .tipoMed(measure.tipoMedida())
                .fecha(measure.timestamp())
                .banderaInvVer(measure.banderaInvVer())
                .actent(measure.actent())
                .qactent(measure.qactent())
                .actsal(measure.actsal())
                .qactsal(measure.qactsal())
                .rq1(measure.rQ1())
                .qrq1(measure.qrQ1())
                .rq2(measure.rQ2())
                .qrq2(measure.qrQ2())
                .rq3(measure.rQ3())
                .qrq3(measure.qrQ3())
                .rq4(measure.rQ4())
                .qrq4(measure.qrQ4())
                .medres1(measure.medres1())
                .qmedres1(measure.qmedres1())
                .medres2(measure.medres2())
                .qmedres2(measure.qmedres2())
                .metodObt(measure.metodObt())
                .temporal(measure.temporal())
                .origen(measure.origen())
                .createdOn(now)
                .createdBy(SYSTEM_USER)
                .build();
    }

    private MedidaCCHEntity toMedidaCch(MeasureRecord.Cch measure, Long clientId) {
        LocalDateTime now = LocalDateTime.now();
        return MedidaCCHEntity.builder()
                .clienteId(clientId)
                .fecha(measure.timestamp())
                .banderaInvVer(measure.banderaInvVer())
                .actent(measure.actent())
                .metod(measure.metod())
                .createdOn(now)
                .createdBy(SYSTEM_USER)
                .build();
    }

    private record ClienteResolution(Long clientId, String tarifa, String errorMessage) {

        static ClienteResolution success(Long clientId, String tarifa) {
            return new ClienteResolution(clientId, tarifa, null);
        }

        static ClienteResolution error(String message) {
            return new ClienteResolution(null, null, message);
        }

        boolean valid() {
            return clientId != null && (errorMessage == null || errorMessage.isBlank());
        }
    }
}



