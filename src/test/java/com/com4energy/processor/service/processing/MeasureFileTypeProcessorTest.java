package com.com4energy.processor.service.processing;

import com.com4energy.processor.model.FailureReason;
import com.com4energy.processor.model.FileRecord;
import com.com4energy.processor.model.BusinessResult;
import com.com4energy.processor.model.FileType;
import com.com4energy.processor.model.QualityStatus;
import com.com4energy.processor.outbox.domain.OutboxEventType;
import com.com4energy.processor.service.measure.MeasureFileParserService;
import com.com4energy.processor.service.measure.MeasureFilenameMetadata;
import com.com4energy.processor.service.measure.MeasureParseResult;
import com.com4energy.processor.service.measure.MeasureRecord;
import com.com4energy.processor.service.measure.persistence.MeasurePersistenceContracts;
import com.com4energy.processor.service.measure.validation.MeasureDefectReportService;
import com.com4energy.processor.service.measure.validation.MeasureRecordValidationChain;
import com.com4energy.processor.service.measure.validation.MeasureRecordValidationResult;
import com.com4energy.processor.service.measure.validation.ValidationMode;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MeasureFileTypeProcessorTest {

    @Test
    void processReturnsFailedWhenPersistenceHasErrors() throws Exception {
        MeasureFileParserService parserService = mock(MeasureFileParserService.class);
        MeasurePersistenceContracts.MeasurePersistencePort persistencePort = mock(MeasurePersistenceContracts.MeasurePersistencePort.class);
        MeasureRecordValidationChain validationChain = mock(MeasureRecordValidationChain.class);
        MeasureDefectReportService defectReportService = mock(MeasureDefectReportService.class);
        MeasureFileTypeProcessor processor = new MeasureFileTypeProcessor(
                parserService, persistencePort, validationChain, defectReportService);

        FileRecord fileRecord = FileRecord.builder()
                .id(10L)
                .originalFilename("P1D_0021_0894_20240104.0")
                .type(FileType.MEDIDA_H_P1)
                .build();

        when(parserService.parse(any(Path.class), eq(FileType.MEDIDA_H_P1))).thenReturn(validParseResult());
        when(validationChain.validate(any(List.class), org.mockito.ArgumentMatchers.eq(ValidationMode.TOLERANT)))
                .thenReturn(new MeasureRecordValidationResult(List.of(hourlyRecord()), List.of()));
        when(persistencePort.persist(any(MeasurePersistenceContracts.PersistMeasuresCommand.class)))
                .thenReturn(new MeasurePersistenceContracts.MeasurePersistenceResult(
                        12,
                        3,
                        0,
                        List.of("No se encontró cliente para CUPS ES001")
                ));
        when(defectReportService.writeValidationAndPersistenceDefectReport(any(), any(), any()))
                .thenReturn(java.util.Optional.of(Path.of("/tmp/P1D_0021.sge_defect.jsonl")));

        FileTypeProcessingResult result = processor.process(fileRecord, Path.of("/tmp/P1D_0021_0894_20240104.0"));

        assertEquals(FileTypeProcessingResult.Status.SUCCEEDED, result.status());
        assertEquals(BusinessResult.PARTIAL_SUCCEEDED, fileRecord.getBusinessResult());
        assertEquals(QualityStatus.WITH_DEFECTS, fileRecord.getQualityStatus());
        assertTrue(fileRecord.getComment().contains("Se generó"));
        assertFalse(result.deferredOutboxEvents().isEmpty());
        assertEquals(12, fileRecord.getProcessedRecords());
        assertEquals(3, fileRecord.getDefectedRecords());
        assertNotNull(fileRecord.getParseDurationMs());

        verify(parserService).parse(any(Path.class), any(FileType.class));
        verify(validationChain).validate(any(List.class), org.mockito.ArgumentMatchers.eq(ValidationMode.TOLERANT));
        verify(persistencePort).persist(any(MeasurePersistenceContracts.PersistMeasuresCommand.class));
    }

    @Test
    void processSkipsPersistenceWhenParserHasErrors() throws Exception {
        MeasureFileParserService parserService = mock(MeasureFileParserService.class);
        MeasurePersistenceContracts.MeasurePersistencePort persistencePort = mock(MeasurePersistenceContracts.MeasurePersistencePort.class);
        MeasureRecordValidationChain validationChain = mock(MeasureRecordValidationChain.class);
        MeasureDefectReportService defectReportService = mock(MeasureDefectReportService.class);
        MeasureFileTypeProcessor processor = new MeasureFileTypeProcessor(
                parserService, persistencePort, validationChain, defectReportService);

        FileRecord fileRecord = FileRecord.builder()
                .id(11L)
                .originalFilename("P1D_0021_0894_20240104.0")
                .type(FileType.MEDIDA_H_P1)
                .build();

        MeasureParseResult parseResultWithErrors = new MeasureParseResult(
                metadata(),
                List.of(hourlyRecord()),
                List.of(new com.com4energy.processor.service.measure.MeasureLineParseError(2, "bad", "Dato inválido"))
        );

        when(parserService.parse(any(Path.class), eq(FileType.MEDIDA_H_P1))).thenReturn(parseResultWithErrors);

        FileTypeProcessingResult result = processor.process(fileRecord, Path.of("/tmp/P1D_0021_0894_20240104.0"));

        assertEquals(FileTypeProcessingResult.Status.FAILED, result.status());
        assertEquals(FailureReason.INVALID_FILE_FORMAT, result.failureReason());
        assertTrue(result.comment().contains("Se generó") || result.comment().contains("reporte no generado"));
        assertEquals(BusinessResult.NOT_PERSISTED, fileRecord.getBusinessResult());
        assertEquals(QualityStatus.WITH_DEFECTS, fileRecord.getQualityStatus());
        assertEquals(1, fileRecord.getProcessedRecords());
        assertEquals(1, fileRecord.getDefectedRecords());
        assertNotNull(fileRecord.getParseDurationMs());

        verify(parserService).parse(any(Path.class), any(FileType.class));
        verify(validationChain, never()).validate(any(List.class), any(ValidationMode.class));
        verify(persistencePort, never()).persist(any(MeasurePersistenceContracts.PersistMeasuresCommand.class));
    }

    @Test
    void processPublishesDefectReportCreatedEventWhenParserHasErrors() throws Exception {
        MeasureFileParserService parserService = mock(MeasureFileParserService.class);
        MeasurePersistenceContracts.MeasurePersistencePort persistencePort = mock(MeasurePersistenceContracts.MeasurePersistencePort.class);
        MeasureRecordValidationChain validationChain = mock(MeasureRecordValidationChain.class);
        MeasureDefectReportService defectReportService = mock(MeasureDefectReportService.class);
        MeasureFileTypeProcessor processor = new MeasureFileTypeProcessor(
                parserService, persistencePort, validationChain, defectReportService);

        FileRecord fileRecord = FileRecord.builder()
                .id(55L)
                .originalFilename("P1D_0021_0894_20240104.0")
                .type(FileType.MEDIDA_H_P1)
                .build();

        MeasureParseResult parseResultWithErrors = new MeasureParseResult(
                metadata(),
                List.of(hourlyRecord()),
                List.of(new com.com4energy.processor.service.measure.MeasureLineParseError(3, "bad;data", "Formato inválido"))
        );
        when(parserService.parse(any(Path.class), eq(FileType.MEDIDA_H_P1))).thenReturn(parseResultWithErrors);
        when(defectReportService.writeParseDefectReport(any(), any()))
                .thenReturn(java.util.Optional.of(Path.of("/tmp/P1D_0021.sge_defect.jsonl")));

        FileTypeProcessingResult result = processor.process(fileRecord, Path.of("/tmp/P1D_0021_0894_20240104.0"));

        assertEquals(FileTypeProcessingResult.Status.FAILED, result.status());
        assertEquals(1, result.deferredOutboxEvents().size());
        assertEquals(OutboxEventType.FILE_DEFECT_REPORT_CREATED, result.deferredOutboxEvents().get(0).eventType());
        assertEquals("parse", result.deferredOutboxEvents().get(0).phase());
    }

    @Test
    void processPublishesDefectReportCreatedEventWhenValidationHasErrors() throws Exception {
        MeasureFileParserService parserService = mock(MeasureFileParserService.class);
        MeasurePersistenceContracts.MeasurePersistencePort persistencePort = mock(MeasurePersistenceContracts.MeasurePersistencePort.class);
        MeasureRecordValidationChain validationChain = mock(MeasureRecordValidationChain.class);
        MeasureDefectReportService defectReportService = mock(MeasureDefectReportService.class);
        MeasureFileTypeProcessor processor = new MeasureFileTypeProcessor(
                parserService, persistencePort, validationChain, defectReportService);

        FileRecord fileRecord = FileRecord.builder()
                .id(77L)
                .originalFilename("P1D_0021_0894_20240104.0")
                .type(FileType.MEDIDA_H_P1)
                .build();

        when(parserService.parse(any(Path.class), eq(FileType.MEDIDA_H_P1))).thenReturn(validParseResult());
        when(validationChain.validate(any(List.class), org.mockito.ArgumentMatchers.eq(ValidationMode.TOLERANT)))
                .thenReturn(new MeasureRecordValidationResult(
                        List.of(),
                        List.of(new com.com4energy.processor.service.measure.validation.MeasureRecordValidationError(
                                2, "CUPS_FORMAT", "CUPS con formato inválido: ES001", "ES001;11;..."))
                ));
        when(persistencePort.persist(any(MeasurePersistenceContracts.PersistMeasuresCommand.class)))
                .thenReturn(new MeasurePersistenceContracts.MeasurePersistenceResult(0, 0, 0, List.of()));
        when(defectReportService.writeValidationAndPersistenceDefectReport(any(), any(), any()))
                .thenReturn(java.util.Optional.of(Path.of("/tmp/P1D_0021.sge_defect.jsonl")));

        FileTypeProcessingResult result = processor.process(fileRecord, Path.of("/tmp/P1D_0021_0894_20240104.0"));

        assertEquals(FileTypeProcessingResult.Status.FAILED, result.status());
        assertEquals(1, result.deferredOutboxEvents().size());
        assertEquals(OutboxEventType.FILE_DEFECT_REPORT_CREATED, result.deferredOutboxEvents().get(0).eventType());
        assertEquals("validation", result.deferredOutboxEvents().get(0).phase());
    }

    @Test
    void processPublishesQuarantineEventWhenBinarySplitIsolatesRecords() throws Exception {
        MeasureFileParserService parserService = mock(MeasureFileParserService.class);
        MeasurePersistenceContracts.MeasurePersistencePort persistencePort = mock(MeasurePersistenceContracts.MeasurePersistencePort.class);
        MeasureRecordValidationChain validationChain = mock(MeasureRecordValidationChain.class);
        MeasureDefectReportService defectReportService = mock(MeasureDefectReportService.class);
        MeasureFileTypeProcessor processor = new MeasureFileTypeProcessor(
                parserService, persistencePort, validationChain, defectReportService);

        FileRecord fileRecord = FileRecord.builder()
                .id(99L)
                .originalFilename("P1D_0021_0894_20240104.0")
                .type(FileType.MEDIDA_H_P1)
                .build();

        // Persistencia devuelve 1 registro fallido (binary split isolation)
        MeasureRecord.Hourly failedRecord = hourlyRecord();
        when(parserService.parse(any(Path.class), eq(FileType.MEDIDA_H_P1))).thenReturn(validParseResult());
        when(validationChain.validate(any(List.class), org.mockito.ArgumentMatchers.eq(ValidationMode.TOLERANT)))
                .thenReturn(new MeasureRecordValidationResult(List.of(failedRecord), List.of()));
        when(persistencePort.persist(any(MeasurePersistenceContracts.PersistMeasuresCommand.class)))
                .thenReturn(new MeasurePersistenceContracts.MeasurePersistenceResult(
                        0, 0, 0,
                        List.of(),                   // sin errores de validación/cliente
                        List.of(failedRecord),       // 1 registro aislado por binary split
                        null
                ));
        when(defectReportService.writeQuarantineDefectReport(any(), any()))
                .thenReturn(java.util.Optional.of(java.nio.file.Path.of("/tmp/quarantine.jsonl")));

        FileTypeProcessingResult result = processor.process(fileRecord, Path.of("/tmp/P1D_0021_0894_20240104.0"));

        // El proceso debe continuar como éxito (el registro malo quedó aislado, no bloqueó el resto)
        assertEquals(FileTypeProcessingResult.Status.FAILED, result.status());
        assertEquals(BusinessResult.NOT_PERSISTED, fileRecord.getBusinessResult());
        assertEquals(QualityStatus.WITH_DEFECTS, fileRecord.getQualityStatus());
        assertEquals(1, fileRecord.getDefectedRecords());

        assertEquals(1, result.deferredOutboxEvents().size());
        assertEquals(OutboxEventType.FILE_PERSISTENCE_QUARANTINE, result.deferredOutboxEvents().get(0).eventType());
        assertEquals(1, result.deferredOutboxEvents().get(0).failedRecordCount());

        // Verifica que se escribió el archivo de cuarentena
        verify(defectReportService).writeQuarantineDefectReport(
                org.mockito.ArgumentMatchers.eq("P1D_0021_0894_20240104.0"),
                org.mockito.ArgumentMatchers.anyList()
        );
    }

    private MeasureParseResult validParseResult() {
        return new MeasureParseResult(metadata(), List.of(hourlyRecord()), List.of());
    }

    private MeasureFilenameMetadata metadata() {
        return new MeasureFilenameMetadata("P1D_0021_0894_20240104.0", FileType.MEDIDA_H_P1);
    }

    private MeasureRecord.Hourly hourlyRecord() {
        return new MeasureRecord.Hourly(
                "ES001",
                LocalDateTime.of(2025, 1, 1, 0, 0),
                11,
                0f, 1f, 0f, 0f, 0f, 0f, 0f, 0f,
                0f, 0f, 0f, 0f, 0f,
                0f, 0f, 0f, 0f,
                1, 0,
                "P1D_0021_0894_20240104.0",
                "ES001;11;2025/01/01 00:00:00;..."
        );
    }
}

