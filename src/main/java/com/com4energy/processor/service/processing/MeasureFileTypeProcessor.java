package com.com4energy.processor.service.processing;

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
            FileType.MEDIDA_QH_P1,
            FileType.MEDIDA_QH_P2
            // F5 y P5 deshabilitados de momento - cliente aún por definir
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
            MeasureParseResult result = measureFileParserService.parse(processingPath);
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
            String measureType = fileRecord.getType() != null
                    ? fileRecord.getType().name()
                    : result.metadata().kind().name();
            String destinationStore = resolveDestinationStore(result.metadata().kind().name());

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


            log.info(
                    "event=measure_file_processed fileId={} filename='{}' measureType={} total={} persisted={} defects={} skipped={} targetTable={} totalMs={} parseMs={} persistMs={}",
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
            );

            // Cuarentena: registros aislados por binary split — flujo EXCEPCIONAL
            // Se reportan por separado (.sge_quarantine.jsonl) y se publican al outbox
            if (persistenceResult.hasFailedRecords()) {
                buildQuarantineDeferredEvent(fileRecord, persistenceResult).ifPresent(deferredOutboxEvents::add);
            }

            // Errores de validación/persistencia — flujo NORMAL con incidencias
            if (hasValidationOrPersistenceErrors) {
                int totalIncidents = validationErrorCount + persistenceResult.errorCount();
                Optional<Path> defectPath = measureDefectReportService.writeValidationAndPersistenceDefectReport(
                        fileRecord.getOriginalFilename(),
                        validationResult.errors(),
                        persistenceResult.errors()
                );
                String comment = buildDefectComment(totalIncidents, defectPath);
                deferredOutboxEvents.addAll(buildDefectReportDeferredEvents("validation", totalIncidents, defectPath));

                if (persistedMeasures > 0) {
                    fileRecord.setComment(comment);
                    return FileTypeProcessingResult.success(deferredOutboxEvents);
                }
                return FileTypeProcessingResult.failed(FailureReason.INVALID_FILE_FORMAT, comment, deferredOutboxEvents);
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

    private String resolveDestinationStore(String measureKind) {
        return switch (measureKind) {
            case "P1" -> "medida_h";
            case "P2" -> "medida_qh";
            case "F5" -> "medida_legacy";
            case "P5" -> "medida_cch";
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

}
