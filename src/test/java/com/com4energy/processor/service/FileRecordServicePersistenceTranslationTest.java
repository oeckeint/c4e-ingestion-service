package com.com4energy.processor.service;

import com.com4energy.processor.config.FeatureFlagService;
import com.com4energy.processor.config.InstanceIdentifier;
import com.com4energy.processor.config.properties.FileProcessingJobProperties;
import com.com4energy.processor.exception.DuplicateHashPersistenceException;
import com.com4energy.processor.model.FileOrigin;
import com.com4energy.processor.model.FileRecord;
import com.com4energy.processor.model.FileStatus;
import com.com4energy.processor.outbox.domain.OutboxEventType;
import com.com4energy.processor.outbox.repository.OutboxEventRepository;
import com.com4energy.processor.repository.FileRecordRepository;
import com.com4energy.processor.service.processing.FileTypeProcessorRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLIntegrityConstraintViolationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FileRecordServicePersistenceTranslationTest {

    @Test
    void saveTranslatesDuplicateHashPersistenceConflictToDomainException() {
        FeatureFlagService featureFlagService = mock(FeatureFlagService.class);
        FileRecordRepository repository = mock(FileRecordRepository.class);

        FileRecordService service = new FileRecordService(
                featureFlagService,
                repository,
                mock(FileTypeProcessorRegistry.class),
                mock(FileProcessingJobProperties.class),
                mock(InstanceIdentifier.class),
                mock(OutboxEventRepository.class)
        );

        when(featureFlagService.isPersistenceEnabled()).thenReturn(true);

        SQLIntegrityConstraintViolationException sqlEx =
                new SQLIntegrityConstraintViolationException("Duplicate entry", "23000", 1062);
        when(repository.save(org.mockito.ArgumentMatchers.any(FileRecord.class)))
                .thenThrow(new DataIntegrityViolationException("constraint failed", sqlEx));

        FileRecord fileRecord = FileRecord.builder()
                .hash("abc123")
                .origin(FileOrigin.API)
                .build();

        DuplicateHashPersistenceException ex = assertThrows(
                DuplicateHashPersistenceException.class,
                () -> service.save(fileRecord, FileStatus.PENDING)
        );

        assertEquals("abc123", ex.getHash());
    }

    @Test
    void savePendingPublishesFileRegisteredOutboxEvent() {
        FeatureFlagService featureFlagService = mock(FeatureFlagService.class);
        FileRecordRepository repository = mock(FileRecordRepository.class);
        OutboxEventRepository outboxEventRepository = mock(OutboxEventRepository.class);

        FileRecordService service = new FileRecordService(
                featureFlagService,
                repository,
                mock(FileTypeProcessorRegistry.class),
                mock(FileProcessingJobProperties.class),
                mock(InstanceIdentifier.class),
                outboxEventRepository
        );

        when(featureFlagService.isPersistenceEnabled()).thenReturn(true);
        when(repository.save(org.mockito.ArgumentMatchers.any(FileRecord.class))).thenAnswer(invocation -> {
            FileRecord saved = invocation.getArgument(0);
            saved.setId(99L);
            return saved;
        });

        FileRecord fileRecord = FileRecord.builder()
                .originalFilename("P1D_1234_5678_20240104.0")
                .origin(FileOrigin.API)
                .hash("hash-99")
                .build();

        service.save(fileRecord, FileStatus.PENDING);

        verify(outboxEventRepository).save(org.mockito.ArgumentMatchers.argThat(event ->
                OutboxEventType.FILE_REGISTERED.name().equals(event.getEventType())
                        && event.getPayload().contains("\"status\":\"PENDING\"")
                        && event.getPayload().contains("\"sourceId\":\"99\"")
        ));
    }
}

