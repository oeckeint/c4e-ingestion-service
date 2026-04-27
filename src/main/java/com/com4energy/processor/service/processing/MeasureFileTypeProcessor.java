package com.com4energy.processor.service.processing;

import com.com4energy.i18n.core.Messages;
import com.com4energy.processor.common.LogsCommonMessageKey;
import com.com4energy.processor.model.FailureReason;
import com.com4energy.processor.model.FileRecord;
import com.com4energy.processor.model.FileType;
import com.com4energy.processor.model.BusinessResult;
import com.com4energy.processor.model.QualityStatus;
import com.com4energy.processor.service.measure.MeasureFileParserService;
import com.com4energy.processor.service.measure.MeasureParseResult;
import com.com4energy.processor.service.measure.persistence.MeasurePersistenceContracts;
import com.com4energy.processor.service.measure.validation.MeasureDefectReportService;
import com.com4energy.processor.service.measure.validation.MeasureRecordValidationChain;
import com.com4energy.processor.service.measure.validation.MeasureRecordValidationResult;
import com.com4energy.processor.service.measure.validation.ValidationMode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
public class MeasureFileTypeProcessor implements FileTypeProcessor {

    private static final Set<FileType> SUPPORTED_TYPES = Set.of(
            FileType.MEDIDA_H_P1,
            FileType.MEDIDA_QH_P2,
            FileType.MEDIDA_CCH_F5
    );

    private final MeasureFileParserService measureFileParserService;
    private final MeasurePersistenceContracts.MeasurePersistencePort measurePersistencePort;
    private final MeasureRecordValidationChain measureRecordValidationChain;
    private final MeasureDefectReportService measureDefectReportService;

    public MeasureFileTypeProcessor(
            MeasureFileParserService measureFileParserService,
            MeasurePersistenceContracts.MeasurePersistencePort measurePersistencePort,
            MeasureRecordValidationChain measureRecordValidationChain,
            MeasureDefectReportService measureDefectReportService
    ) {
        this.measureFileParserService = measureFileParserService;
        this.measurePersistencePort = measurePersistencePort;
        this.measureRecordValidationChain = measureRecordValidationChain;
        this.measureDefectReportService = measureDefectReportService;
    }

    @Override
    public Set<FileType> supportedTypes() {
        return SUPPORTED_TYPES;
    }

    @Override
    public FileTypeProcessingResult process(FileRecord fileRecord, Path processingPath) {
        long parseStartedAtNanos = System.nanoTime();
        try {
            // Use the FileType from database (already classified at file ingestion)
            // Don't recalculate it - the job just reads and processes based on pre-classified type
            MeasureParseResult result = measureFileParserService.parse(processingPath, fileRecord.getType());
            long parseDurationMs = elapsedMillis(parseStartedAtNanos);
            fileRecord.setParseDurationMs(parseDurationMs);

            if (result.hasErrors()) {
                fileRecord.setProcessedRecords(result.successCount());
                fileRecord.setDefectedRecords(result.errorCount());
                fileRecord.setQualityStatus(QualityStatus.WITH_DEFECTS);
                fileRecord.setBusinessResult(BusinessResult.NOT_PERSISTED);
                Optional<Path> defectPath = measureDefectReportService.writeParseDefectReport(
                        fileRecord.getOriginalFilename(),
                        result.errors()
                );
                String comment = buildDefectComment(result.errorCount(), defectPath);
                return FileTypeProcessingResult.failed(
                        FailureReason.INVALID_FILE_FORMAT,
                        comment,
                        buildDefectReportDeferredEvents("parse", result.errorCount(), defectPath)
                );
            }

            if (result.records().isEmpty()) {
                fileRecord.setProcessedRecords(0);
                fileRecord.setDefectedRecords(0);
                fileRecord.setQualityStatus(QualityStatus.NOT_EVALUATED);
                fileRecord.setBusinessResult(BusinessResult.NOT_PERSISTED);
                return FileTypeProcessingResult.failed(
                        FailureReason.INVALID_FILE_FORMAT,
                        "El archivo no contiene registros de medidas procesables"
                );
            }

            MeasureRecordValidationResult validationResult = measureRecordValidationChain.validate(
                    result.records(),
                    ValidationMode.TOLERANT
            );
            int validationErrorCount = validationResult.errorCount();

            long persistStartedAtNanos = System.nanoTime();
            MeasurePersistenceContracts.MeasurePersistenceResult persistenceResult = measurePersistencePort.persist(
                    new MeasurePersistenceContracts.PersistMeasuresCommand(
                            fileRecord.getId(),
                            fileRecord.getOriginalFilename(),
                            validationResult.validRecords()
                    )
            );
            long persistDurationMs = elapsedMillis(persistStartedAtNanos);

            int totalMeasuresInFile = result.successCount();
            int persistedMeasures = persistenceResult.persistedCount();
            int defectCount = validationErrorCount + persistenceResult.errorCount() + persistenceResult.failedRecords().size();
            int skippedMeasures = persistenceResult.skippedCount();
            boolean hasValidationOrPersistenceErrors = validationResult.hasErrors() || persistenceResult.hasErrors();
            boolean hasQuarantineRecords = persistenceResult.hasFailedRecords();
            boolean hasDefects = hasValidationOrPersistenceErrors || hasQuarantineRecords;
            List<FileTypeProcessingResult.DeferredOutboxEvent> deferredOutboxEvents = new ArrayList<>();
            long totalProcessingMs = elapsedMillis(parseStartedAtNanos);
            FileType resolvedMeasureType = fileRecord.getType() != null
                    ? fileRecord.getType()
                    : result.metadata().kind();
            String measureType = resolvedMeasureType.name();
            String destinationStore = resolveDestinationStore(resolvedMeasureType);

            fileRecord.setProcessedRecords(persistedMeasures);
            fileRecord.setDefectedRecords(defectCount);
            fileRecord.setQualityStatus(hasDefects ? QualityStatus.WITH_DEFECTS : QualityStatus.CLEAN);
            if (!hasDefects) {
                fileRecord.setBusinessResult(BusinessResult.FULLY_SUCCEEDED);
            } else if (persistedMeasures > 0) {
                fileRecord.setBusinessResult(BusinessResult.PARTIAL_SUCCEEDED);
            } else {
                fileRecord.setBusinessResult(BusinessResult.NOT_PERSISTED);
            }


            log.info(Messages.format(
                    LogsCommonMessageKey.MEASURE_FILE_PROCESSED,
                    fileRecord.getId(),
                    fileRecord.getOriginalFilename(),
                    measureType,
                    totalMeasuresInFile,
                    persistedMeasures,
                    defectCount,
                    skippedMeasures,
                    destinationStore,
                    totalProcessingMs,
                    parseDurationMs,
                    persistDurationMs
            ));

            // Cuarentena: registros aislados por binary split — flujo EXCEPCIONAL
            // Se reportan por separado (.sge_quarantine.jsonl) y se publican al outbox
            if (persistenceResult.hasFailedRecords()) {
                buildQuarantineDeferredEvent(fileRecord, persistenceResult).ifPresent(deferredOutboxEvents::add);
            }

            Optional<FileTypeProcessingResult> defectResult = handleValidationAndPersistenceErrors(
                    fileRecord,
                    validationResult,
                    persistenceResult,
                    hasValidationOrPersistenceErrors,
                    persistedMeasures,
                    deferredOutboxEvents
            );
            if (defectResult.isPresent()) {
                return defectResult.get();
            }

            if (hasQuarantineRecords && persistedMeasures == 0) {
                String comment = "No se pudo persistir ninguna medida; registros aislados en cuarentena por persistencia.";
                fileRecord.setComment(comment);
                return FileTypeProcessingResult.failed(FailureReason.INVALID_FILE_FORMAT, comment, deferredOutboxEvents);
            }

            return FileTypeProcessingResult.success(deferredOutboxEvents);
        } catch (RuntimeException | java.io.IOException ex) {
            fileRecord.setParseDurationMs(elapsedMillis(parseStartedAtNanos));
            throw new IllegalStateException("Error processing measure file '" + fileRecord.getOriginalFilename() + "'", ex);
        }
    }

    private long elapsedMillis(long startedAtNanos) {
        return (System.nanoTime() - startedAtNanos) / 1_000_000;
    }

    private String resolveDestinationStore(FileType measureType) {
        if (measureType == null) {
            return "desconocido";
        }

        return switch (measureType) {
            case MEDIDA_H_P1 -> "medida_h";
            case MEDIDA_QH_P2 -> "medida_qh";
            case MEDIDA_CCH_F5 -> "medida_cch";
            default -> "desconocido";
        };
    }

    private String buildDefectComment(int incidentCount, Optional<Path> reportPath) {
        String filename = reportPath.map(p -> p.getFileName().toString()).orElse("(reporte no generado)");
        return "Se generó " + filename + " con " + incidentCount + " incidencia(s)";
    }

    private List<FileTypeProcessingResult.DeferredOutboxEvent> buildDefectReportDeferredEvents(
            String phase,
            int incidentCount,
            Optional<Path> defectPath
    ) {
        return defectPath
                .map(path -> List.of(FileTypeProcessingResult.DeferredOutboxEvent.defectReportCreated(phase, incidentCount, path)))
                .orElseGet(List::of);
    }

    /**
     * Maneja registros aislados por binary split.
     * Flujo excepcional: el registro pasó validación pero falló en BD (constraint, FK, etc.).
     * → Escribe .sge_quarantine.jsonl (SEPARADO del .sge_defect.jsonl de validación)
     * → Publica FILE_PERSISTENCE_QUARANTINE al outbox para auditoría y posible retry
     */
    private Optional<FileTypeProcessingResult.DeferredOutboxEvent> buildQuarantineDeferredEvent(
            FileRecord fileRecord,
            MeasurePersistenceContracts.MeasurePersistenceResult persistenceResult
    ) {
        int failedCount = persistenceResult.failedRecords().size();
        log.warn(
                "Binary split quarantine: {} record(s) isolated for file '{}'. Publishing quarantine event.",
                failedCount,
                fileRecord.getOriginalFilename()
        );

        List<MeasureDefectReportService.PersistenceFailedRecord> quarantineEntries =
                persistenceResult.failedRecords().stream()
                        .map(failedRecord -> new MeasureDefectReportService.PersistenceFailedRecord(
                                failedRecord,
                                failedRecord.kind().name(),
                                "Binary split isolation — record could not be persisted",
                                failedRecord.cups() + "@" + failedRecord.timestamp()
                        ))
                        .toList();

        Optional<Path> quarantinePath = measureDefectReportService.writeQuarantineDefectReport(
                fileRecord.getOriginalFilename(),
                quarantineEntries
        );

        log.info(
                "Quarantine report prepared for deferred outbox publish: fileId={}, failedRecords={}, path={}",
                fileRecord.getId(),
                failedCount,
                quarantinePath.map(p -> p.toAbsolutePath().toString()).orElse("N/A")
        );

        return quarantinePath.map(path -> FileTypeProcessingResult.DeferredOutboxEvent.persistenceQuarantine(failedCount, path));
    }

    private Optional<FileTypeProcessingResult> handleValidationAndPersistenceErrors(
            FileRecord fileRecord,
            MeasureRecordValidationResult validationResult,
            MeasurePersistenceContracts.MeasurePersistenceResult persistenceResult,
            boolean hasValidationOrPersistenceErrors,
            int persistedMeasures,
            List<FileTypeProcessingResult.DeferredOutboxEvent> deferredOutboxEvents
    ) {
        if (!hasValidationOrPersistenceErrors) {
            return Optional.empty();
        }

        int totalIncidents = validationResult.errorCount() + persistenceResult.errorCount();
        Optional<Path> defectPath = measureDefectReportService.writeValidationAndPersistenceDefectReport(
                fileRecord.getOriginalFilename(),
                validationResult.errors(),
                persistenceResult.errors()
        );
        String comment = buildDefectComment(totalIncidents, defectPath);
        deferredOutboxEvents.addAll(buildDefectReportDeferredEvents("validation", totalIncidents, defectPath));

        if (persistedMeasures > 0) {
            fileRecord.setComment(comment);
            return Optional.of(FileTypeProcessingResult.success(deferredOutboxEvents));
        }

        return Optional.of(FileTypeProcessingResult.failed(FailureReason.INVALID_FILE_FORMAT, comment, deferredOutboxEvents));
    }

}
