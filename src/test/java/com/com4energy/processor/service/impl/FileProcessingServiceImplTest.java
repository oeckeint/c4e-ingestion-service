package com.com4energy.processor.service.impl;

import com.com4energy.processor.config.InstanceIdentifier;
import com.com4energy.processor.config.properties.FileUploadProperties;
import com.com4energy.processor.config.properties.features.ProcessorFeatures;
import com.com4energy.processor.model.FailureReason;
import com.com4energy.processor.model.FileRecord;
import com.com4energy.processor.model.FileStatus;
import com.com4energy.processor.model.FileType;
import com.com4energy.processor.outbox.domain.OutboxEventType;
import com.com4energy.processor.outbox.repository.OutboxEventRepository;
import com.com4energy.processor.service.FileRecordService;
import com.com4energy.processor.service.IncidentNotificationService;
import com.com4energy.processor.service.measure.MeasureFileParserService;
import com.com4energy.processor.service.measure.persistence.MeasurePersistenceContracts;
import com.com4energy.processor.service.measure.validation.MeasureDefectReportService;
import com.com4energy.processor.service.measure.validation.MeasureRecordValidationChain;
import com.com4energy.processor.service.processing.FileTypeProcessingResult;
import com.com4energy.processor.service.processing.FileTypeProcessor;
import com.com4energy.processor.service.processing.FileTypeProcessorRegistry;
import com.com4energy.processor.service.processing.MeasureFileTypeProcessor;
import com.com4energy.processor.util.FileStorageUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FileProcessingServiceImplTest {

    private static final String MEASURE_FILENAME = "P1AA_BBBB_CCCC_DDDDDDD.0";

    @TempDir
    Path tempDir;

    @Test
    void processFileMovesSuccessfulMeasureToProcessed() throws IOException {
        Path pendingFile = createPendingFile(
                "ES123;1;2025/01/01 00:00;1.0;2.0;3.0;4.0;5.0;6.0;7.0;8.0;9.0;10.0;11.0;12.0;13.0;14.0;15.0;16.0;17.0;18;19");

        FileRecordService fileRecordService = mock(FileRecordService.class);
        IncidentNotificationService incidentNotificationService = mock(IncidentNotificationService.class);
        InstanceIdentifier instanceIdentifier = mock(InstanceIdentifier.class);
        when(instanceIdentifier.getInstanceId()).thenReturn("instance-a");
        when(fileRecordService.saveIfOwnedBy(org.mockito.ArgumentMatchers.any(FileRecord.class), org.mockito.ArgumentMatchers.eq("instance-a")))
                .thenReturn(true);
        when(fileRecordService.releaseLockIfOwnedBy(org.mockito.ArgumentMatchers.any(FileRecord.class), org.mockito.ArgumentMatchers.eq("instance-a")))
                .thenReturn(true);
        OutboxEventRepository outboxEventRepository = mock(OutboxEventRepository.class);

        FileProcessingServiceImpl service = newService(
                fileRecordService,
                incidentNotificationService,
                instanceIdentifier,
                new FileTypeProcessorRegistry(List.of(measureProcessorWithStubPersistence())),
                outboxEventRepository
        );

        FileRecord fileRecord = claimedMeasureRecord(pendingFile);

        service.processFile(fileRecord);

        assertEquals(FileStatus.SUCCEEDED, fileRecord.getStatus());
        assertNotNull(fileRecord.getProcessedAt());
        assertEquals(1, fileRecord.getProcessedRecords());
        assertEquals(0, fileRecord.getDefectedRecords());
        assertTrue(fileRecord.getParseDurationMs() != null && fileRecord.getParseDurationMs() >= 0);
        assertTrue(fileRecord.getProcessingDurationMs() != null && fileRecord.getProcessingDurationMs() >= 0);
        assertTrue(fileRecord.getFinalPath().contains("processed"));
        assertTrue(Files.exists(Path.of(fileRecord.getFinalPath())));
        assertFalse(Files.exists(pendingFile));
        verify(fileRecordService, atLeast(2)).saveIfOwnedBy(org.mockito.ArgumentMatchers.eq(fileRecord), org.mockito.ArgumentMatchers.eq("instance-a"));
        verify(incidentNotificationService, never()).notifyProcessingError(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void processFileMovesInvalidMeasureToFailed() throws IOException {
        Path pendingFile = createPendingFile(
                "ES123;1;BAD_DATE;1.0;2.0;3.0;4.0;5.0;6.0;7.0;8.0;9.0;10.0;11.0;12.0;13.0;14.0;15.0;16.0;17.0;18;19");

        FileRecordService fileRecordService = mock(FileRecordService.class);
        IncidentNotificationService incidentNotificationService = mock(IncidentNotificationService.class);
        InstanceIdentifier instanceIdentifier = mock(InstanceIdentifier.class);
        when(instanceIdentifier.getInstanceId()).thenReturn("instance-a");
        when(fileRecordService.saveIfOwnedBy(org.mockito.ArgumentMatchers.any(FileRecord.class), org.mockito.ArgumentMatchers.eq("instance-a")))
                .thenReturn(true);
        when(fileRecordService.releaseLockIfOwnedBy(org.mockito.ArgumentMatchers.any(FileRecord.class), org.mockito.ArgumentMatchers.eq("instance-a")))
                .thenReturn(true);
        OutboxEventRepository outboxEventRepository = mock(OutboxEventRepository.class);

        FileProcessingServiceImpl service = newService(
                fileRecordService,
                incidentNotificationService,
                instanceIdentifier,
                new FileTypeProcessorRegistry(List.of(measureProcessorWithStubPersistence())),
                outboxEventRepository
        );

        FileRecord fileRecord = claimedMeasureRecord(pendingFile);

        service.processFile(fileRecord);

        assertEquals(FileStatus.FAILED, fileRecord.getStatus());
        assertEquals(FailureReason.INVALID_FILE_FORMAT, fileRecord.getFailureReason());
        assertEquals(0, fileRecord.getProcessedRecords());
        assertEquals(1, fileRecord.getDefectedRecords());
        assertTrue(fileRecord.getParseDurationMs() != null && fileRecord.getParseDurationMs() >= 0);
        assertTrue(fileRecord.getProcessingDurationMs() != null && fileRecord.getProcessingDurationMs() >= 0);
        assertTrue(fileRecord.getComment().contains("Se generó") || fileRecord.getComment().contains("reporte no generado"));
        assertTrue(fileRecord.getFinalPath().contains("failed"));
        assertTrue(Files.exists(Path.of(fileRecord.getFinalPath())));
        assertFalse(Files.exists(pendingFile));
        verify(incidentNotificationService, never()).notifyProcessingError(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void processFileReturnsFileToPendingWhenUnexpectedErrorIsRetryable() throws IOException {
        Path pendingFile = createPendingFile(
                "ES123;1;2025/01/01 00:00;1.0;2.0;3.0;4.0;5.0;6.0;7.0;8.0;9.0;10.0;11.0;12.0;13.0;14.0;15.0;16.0;17.0;18;19");

        FileRecordService fileRecordService = mock(FileRecordService.class);
        IncidentNotificationService incidentNotificationService = mock(IncidentNotificationService.class);
        InstanceIdentifier instanceIdentifier = mock(InstanceIdentifier.class);
        when(instanceIdentifier.getInstanceId()).thenReturn("instance-a");
        when(fileRecordService.saveIfOwnedBy(org.mockito.ArgumentMatchers.any(FileRecord.class), org.mockito.ArgumentMatchers.eq("instance-a")))
                .thenReturn(true);
        when(fileRecordService.releaseLockIfOwnedBy(org.mockito.ArgumentMatchers.any(FileRecord.class), org.mockito.ArgumentMatchers.eq("instance-a")))
                .thenReturn(true);
        OutboxEventRepository outboxEventRepository = mock(OutboxEventRepository.class);

        FileTypeProcessor explodingProcessor = new FileTypeProcessor() {
            @Override
            public java.util.Set<FileType> supportedTypes() {
                return java.util.Set.of(FileType.MEDIDA_QH_P1);
            }

            @Override
            public FileTypeProcessingResult process(FileRecord fileRecord, Path processingPath) {
                throw new IllegalStateException("boom");
            }
        };

        FileProcessingServiceImpl service = newService(
                fileRecordService,
                incidentNotificationService,
                instanceIdentifier,
                new FileTypeProcessorRegistry(List.of(explodingProcessor)),
                outboxEventRepository
        );

        FileRecord fileRecord = claimedMeasureRecord(pendingFile);

        service.processFile(fileRecord);

        assertEquals(FileStatus.RETRY, fileRecord.getStatus());
        assertEquals(FailureReason.PROCESSING_ERROR, fileRecord.getFailureReason());
        assertEquals(1, fileRecord.getRetryCount());
        assertTrue(fileRecord.getFinalPath().contains("pending"));
        assertTrue(Files.exists(Path.of(fileRecord.getFinalPath())));
        verify(incidentNotificationService).notifyProcessingError(org.mockito.ArgumentMatchers.eq(fileRecord), org.mockito.ArgumentMatchers.any(Exception.class));
    }

    @Test
    void processFileMarksAsFailedWhenRemediationAlsoFails() throws IOException {
        Path processingDir = Path.of(uploadProperties().processingPath());
        Files.createDirectories(processingDir);
        Path processingFile = Files.writeString(processingDir.resolve(MEASURE_FILENAME), "content");

        FileRecordService fileRecordService = mock(FileRecordService.class);
        IncidentNotificationService incidentNotificationService = mock(IncidentNotificationService.class);
        InstanceIdentifier instanceIdentifier = mock(InstanceIdentifier.class);
        when(instanceIdentifier.getInstanceId()).thenReturn("instance-a");
        when(fileRecordService.saveIfOwnedBy(org.mockito.ArgumentMatchers.any(FileRecord.class), org.mockito.ArgumentMatchers.eq("instance-a")))
                .thenReturn(true);
        when(fileRecordService.releaseLockIfOwnedBy(org.mockito.ArgumentMatchers.any(FileRecord.class), org.mockito.ArgumentMatchers.eq("instance-a")))
                .thenReturn(true);
        OutboxEventRepository outboxEventRepository = mock(OutboxEventRepository.class);

        FileTypeProcessor explodingProcessor = new FileTypeProcessor() {
            @Override
            public java.util.Set<FileType> supportedTypes() {
                return java.util.Set.of(FileType.MEDIDA_QH_P1);
            }

            @Override
            public FileTypeProcessingResult process(FileRecord fileRecord, Path processingPath) {
                throw new IllegalStateException("boom");
            }
        };

        ProcessorFeatures processorFeatures = new ProcessorFeatures();
        processorFeatures.setMaxRetries(3);
        FileStorageUtil storageUtil = mock(FileStorageUtil.class);
        when(storageUtil.moveFileToPending(org.mockito.ArgumentMatchers.any(java.io.File.class))).thenReturn(null);

        FileProcessingServiceImpl service = new FileProcessingServiceImpl(
                fileRecordService,
                storageUtil,
                incidentNotificationService,
                instanceIdentifier,
                new FileTypeProcessorRegistry(List.of(explodingProcessor)),
                processorFeatures,
                uploadProperties(),
                outboxEventRepository
        );

        FileRecord fileRecord = claimedMeasureRecord(processingFile);
        fileRecord.setStatus(FileStatus.PROCESSING);

        service.processFile(fileRecord);

        assertEquals(FileStatus.FAILED, fileRecord.getStatus());
        assertEquals(FailureReason.PROCESSING_ERROR, fileRecord.getFailureReason());
        assertTrue(fileRecord.getComment().contains("remediation failed"));
        verify(fileRecordService, atLeast(1)).saveIfOwnedBy(org.mockito.ArgumentMatchers.eq(fileRecord), org.mockito.ArgumentMatchers.eq("instance-a"));
        verify(incidentNotificationService).notifyProcessingError(org.mockito.ArgumentMatchers.eq(fileRecord), org.mockito.ArgumentMatchers.any(Exception.class));
    }

    @Test
    void processFileStopsWhenOwnershipIsLostBeforeFirstPersist() throws IOException {
        Path pendingFile = createPendingFile(
                "ES123;1;2025/01/01 00:00;1.0;2.0;3.0;4.0;5.0;6.0;7.0;8.0;9.0;10.0;11.0;12.0;13.0;14.0;15.0;16.0;17.0;18;19");

        FileRecordService fileRecordService = mock(FileRecordService.class);
        IncidentNotificationService incidentNotificationService = mock(IncidentNotificationService.class);
        InstanceIdentifier instanceIdentifier = mock(InstanceIdentifier.class);
        when(instanceIdentifier.getInstanceId()).thenReturn("instance-a");
        when(fileRecordService.saveIfOwnedBy(org.mockito.ArgumentMatchers.any(FileRecord.class), org.mockito.ArgumentMatchers.eq("instance-a")))
                .thenReturn(false);
        when(fileRecordService.releaseLockIfOwnedBy(org.mockito.ArgumentMatchers.any(FileRecord.class), org.mockito.ArgumentMatchers.eq("instance-a")))
                .thenReturn(false);
        OutboxEventRepository outboxEventRepository = mock(OutboxEventRepository.class);

        FileProcessingServiceImpl service = newService(
                fileRecordService,
                incidentNotificationService,
                instanceIdentifier,
                new FileTypeProcessorRegistry(List.of(measureProcessorWithStubPersistence())),
                outboxEventRepository
        );

        FileRecord fileRecord = claimedMeasureRecord(pendingFile);

        service.processFile(fileRecord);

        verify(incidentNotificationService, never()).notifyProcessingError(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(fileRecordService).saveIfOwnedBy(org.mockito.ArgumentMatchers.eq(fileRecord), org.mockito.ArgumentMatchers.eq("instance-a"));
        verify(fileRecordService).releaseLockIfOwnedBy(org.mockito.ArgumentMatchers.eq(fileRecord), org.mockito.ArgumentMatchers.eq("instance-a"));
    }

    @Test
    void processFilePublishesDeferredOutboxEventWithFinalSucceededStatus() throws IOException {
        Path pendingFile = createPendingFile("content");

        FileRecordService fileRecordService = mock(FileRecordService.class);
        IncidentNotificationService incidentNotificationService = mock(IncidentNotificationService.class);
        InstanceIdentifier instanceIdentifier = mock(InstanceIdentifier.class);
        OutboxEventRepository outboxEventRepository = mock(OutboxEventRepository.class);
        when(instanceIdentifier.getInstanceId()).thenReturn("instance-a");
        when(fileRecordService.saveIfOwnedBy(org.mockito.ArgumentMatchers.any(FileRecord.class), org.mockito.ArgumentMatchers.eq("instance-a")))
                .thenReturn(true);
        when(fileRecordService.releaseLockIfOwnedBy(org.mockito.ArgumentMatchers.any(FileRecord.class), org.mockito.ArgumentMatchers.eq("instance-a")))
                .thenReturn(true);

        FileTypeProcessor processor = new FileTypeProcessor() {
            @Override
            public java.util.Set<FileType> supportedTypes() {
                return java.util.Set.of(FileType.MEDIDA_QH_P1);
            }

            @Override
            public FileTypeProcessingResult process(FileRecord fileRecord, Path processingPath) {
                fileRecord.setComment("Se generó reporte.sge_defect.jsonl con 2 incidencia(s)");
                return FileTypeProcessingResult.success(List.of(
                        FileTypeProcessingResult.DeferredOutboxEvent.defectReportCreated(
                                "validation",
                                2,
                                Path.of("/tmp/reporte.sge_defect.jsonl")
                        )
                ));
            }
        };

        FileProcessingServiceImpl service = newService(
                fileRecordService,
                incidentNotificationService,
                instanceIdentifier,
                new FileTypeProcessorRegistry(List.of(processor)),
                outboxEventRepository
        );

        FileRecord fileRecord = claimedMeasureRecord(pendingFile);

        service.processFile(fileRecord);

        verify(outboxEventRepository).save(
                org.mockito.ArgumentMatchers.argThat(event ->
                        OutboxEventType.FILE_DEFECT_REPORT_CREATED.name().equals(event.getEventType())
                                && event.getPayload().contains("\"status\":\"SUCCEEDED\"")
                                && event.getPayload().contains("\"phase\":\"validation\"")
                )
        );
    }

    private FileProcessingServiceImpl newService(
            FileRecordService fileRecordService,
            IncidentNotificationService incidentNotificationService,
            InstanceIdentifier instanceIdentifier,
            FileTypeProcessorRegistry registry,
            OutboxEventRepository outboxEventRepository
    ) {
        ProcessorFeatures processorFeatures = new ProcessorFeatures();
        processorFeatures.setMaxRetries(3);
        return new FileProcessingServiceImpl(
                fileRecordService,
                new FileStorageUtil(uploadProperties()),
                incidentNotificationService,
                instanceIdentifier,
                registry,
                processorFeatures,
                uploadProperties(),
                outboxEventRepository
        );
    }

    private FileRecord claimedMeasureRecord(Path filePath) {
        return FileRecord.builder()
                .id(1L)
                .originalFilename(filePath.getFileName().toString())
                .finalFilename(filePath.getFileName().toString())
                .finalPath(filePath.toAbsolutePath().toString())
                .type(FileType.MEDIDA_QH_P1)
                .status(FileStatus.PENDING)
                .retryCount(0)
                .locked(true)
                .lockedBy("instance-a")
                .build();
    }

    private MeasureFileTypeProcessor measureProcessorWithStubPersistence() {
        MeasurePersistenceContracts.MeasurePersistencePort persistencePort = command ->
                new MeasurePersistenceContracts.MeasurePersistenceResult(
                        command.measureRecords().size(),
                        0,
                        0,
                        List.of()
                );
        MeasureRecordValidationChain validationChain = new MeasureRecordValidationChain(List.of());
        MeasureDefectReportService defectReportService = mock(MeasureDefectReportService.class);
        return new MeasureFileTypeProcessor(
                new MeasureFileParserService(), persistencePort, validationChain,
                defectReportService);
    }

    private Path createPendingFile(String content) throws IOException {
        Path pendingDir = Path.of(uploadProperties().pendingPath());
        Files.createDirectories(pendingDir);
        return Files.writeString(pendingDir.resolve(MEASURE_FILENAME), content);
    }

    private FileUploadProperties uploadProperties() {
        return new FileUploadProperties(
                tempDir.resolve("ingestion-service").toString(),
                tempDir.resolve("ingestion-service/pending").toString(),
                tempDir.resolve("ingestion-service/processed").toString(),
                tempDir.resolve("ingestion-service/processing").toString(),
                tempDir.resolve("ingestion-service/duplicates").toString(),
                tempDir.resolve("ingestion-service/failed").toString(),
                tempDir.resolve("ingestion-service/rejected").toString(),
                tempDir.resolve("ingestion-service/archive").toString(),
                tempDir.resolve("ingestion-service/automatic").toString(),
                10L * 1024 * 1024,
                List.of("xml", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9"),
                List.of("application/xml", "text/plain", "application/octet-stream")
        );
    }
}

