package com.com4energy.processor.service.measure.persistence;

import com.com4energy.processor.service.measure.MeasureRecord;

import java.util.List;

/**
 * Contratos de corto plazo para persistir medidas sobre tablas existentes (sistemagestion).
 *
 * Esta capa desacopla el parser del modelo JPA concreto y permite evolucionar a un módulo compartido más adelante.
 */
public final class MeasurePersistenceContracts {

        private MeasurePersistenceContracts() {
        }

    public record PersistMeasuresCommand(
            Long fileRecordId,
            String origin,
            List<MeasureRecord> measureRecords
    ) {
    }

    public record MeasurePersistenceResult(
            int persistedCount,
            int errorCount,
            int skippedCount,
            List<String> errors,
            List<MeasureRecord> failedRecords,
            String quarantineFilePath
    ) {
        public MeasurePersistenceResult {
            errors = List.copyOf(errors);
            failedRecords = failedRecords != null ? List.copyOf(failedRecords) : List.of();
        }

        public MeasurePersistenceResult(
                int persistedCount,
                int errorCount,
                int skippedCount,
                List<String> errors
        ) {
            this(persistedCount, errorCount, skippedCount, errors, List.of(), null);
        }

        public boolean hasErrors() {
            return errorCount > 0;
        }

        public boolean hasFailedRecords() {
            return !failedRecords.isEmpty();
        }
    }

    public interface MeasurePersistencePort {

        MeasurePersistenceResult persist(PersistMeasuresCommand command);
    }
}
