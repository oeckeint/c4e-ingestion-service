package com.com4energy.processor.service;

import com.com4energy.processor.common.InternalServices;
import com.com4energy.processor.config.properties.FileUploadProperties;
import com.com4energy.processor.exception.DuplicateHashPersistenceException;
import com.com4energy.processor.model.FileOrigin;
import com.com4energy.processor.outbox.service.OutboxService;
import com.com4energy.processor.service.dto.FileBatchResult;
import com.com4energy.processor.service.dto.FileContext;
import com.com4energy.processor.service.dto.FileHandlingResult;
import com.com4energy.processor.service.factory.FileContextMessageFactory;
import com.com4energy.processor.util.FileStorageUtil;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FileUploadOrchestratorServiceTest {

    @Test
    void processFilesDetectsIntraBatchDuplicateBeforePersistence() {
        FileRecordService fileRecordService = mock(FileRecordService.class);
        OutboxService outboxService = mock(OutboxService.class);
        FileStorageUtil fileStorageUtil = mock(FileStorageUtil.class);

        FileUploadOrchestratorService service = new FileUploadOrchestratorService(
                testUploadProperties(),
                fileRecordService,
                outboxService,
                fileStorageUtil,
                List.of(),
                mock(FileContextMessageFactory.class)
        );

        MockMultipartFile fileA = new MockMultipartFile("files", "A.xml", "application/xml", "same-content".getBytes());
        MockMultipartFile fileB = new MockMultipartFile("files", "B.xml", "application/xml", "same-content".getBytes());

        when(fileStorageUtil.saveInDiskOverridingExisting(any(Path.class), any(FileContext.class)))
                .thenAnswer(invocation -> FileHandlingResult.initial(invocation.getArgument(1)).withStoredInDisk());

        when(fileRecordService.saveNew(any(FileHandlingResult.class), eq(FileOrigin.API)))
                .thenAnswer(invocation -> ((FileHandlingResult) invocation.getArgument(0)).withPersistedInFileRecords());

        when(outboxService.saveDuplicated(any(FileHandlingResult.class), eq(InternalServices.CHAIN_VALIDATION), eq(FileOrigin.API)))
                .thenAnswer(invocation -> ((FileHandlingResult) invocation.getArgument(0)).withPersistedInOutboxEvent());

        FileBatchResult result = service.processFiles(new MultipartFile[]{fileA, fileB});

        assertEquals(1, result.validFiles().size());
        assertEquals(1, result.duplicatedFiles().size());
        assertEquals(0, result.failedFiles().size());

        verify(fileRecordService, times(1)).saveNew(any(FileHandlingResult.class), eq(FileOrigin.API));
        verify(outboxService, times(1)).saveDuplicated(any(FileHandlingResult.class), eq(InternalServices.CHAIN_VALIDATION), eq(FileOrigin.API));
    }

    @Test
    void processFilesMapsDuplicateHashDomainExceptionAsDuplicatedAndContinuesBatch() {
        FileRecordService fileRecordService = mock(FileRecordService.class);
        OutboxService outboxService = mock(OutboxService.class);
        FileStorageUtil fileStorageUtil = mock(FileStorageUtil.class);

        FileUploadOrchestratorService service = new FileUploadOrchestratorService(
                testUploadProperties(),
                fileRecordService,
                outboxService,
                fileStorageUtil,
                List.of(),
                mock(FileContextMessageFactory.class)
        );

        MockMultipartFile fileA = new MockMultipartFile("files", "A.xml", "application/xml", "content-A".getBytes());
        MockMultipartFile fileB = new MockMultipartFile("files", "B.xml", "application/xml", "content-B".getBytes());

        when(fileStorageUtil.saveInDiskOverridingExisting(any(Path.class), any(FileContext.class)))
                .thenAnswer(invocation -> FileHandlingResult.initial(invocation.getArgument(1)).withStoredInDisk());

        when(fileRecordService.saveNew(any(FileHandlingResult.class), eq(FileOrigin.API)))
                .thenAnswer(invocation -> ((FileHandlingResult) invocation.getArgument(0)).withPersistedInFileRecords())
                .thenThrow(new DuplicateHashPersistenceException("hash-b", new RuntimeException("duplicate hash")));

        when(outboxService.saveDuplicated(any(FileHandlingResult.class), eq(InternalServices.CHAIN_VALIDATION), eq(FileOrigin.API)))
                .thenAnswer(invocation -> ((FileHandlingResult) invocation.getArgument(0)).withPersistedInOutboxEvent());

        FileBatchResult result = service.processFiles(new MultipartFile[]{fileA, fileB});

        assertEquals(1, result.validFiles().size());
        assertEquals(1, result.duplicatedFiles().size());
        assertEquals(0, result.failedFiles().size());

        verify(fileRecordService, times(2)).saveNew(any(FileHandlingResult.class), eq(FileOrigin.API));
        verify(outboxService, times(1)).saveDuplicated(any(FileHandlingResult.class), eq(InternalServices.CHAIN_VALIDATION), eq(FileOrigin.API));
    }

    /**
     * Verifies that when two files with the same content are sent in a single batch,
     * the one whose name is lexicographically smaller (e.g. ".0" before ".1") is always
     * accepted, and the other is marked DUPLICATED_CONTENT — regardless of the order
     * they appear in the request array.
     */
    @Test
    void processFilesAlwaysAcceptsLowerRevisionWhenContentIsDuplicated() {
        FileRecordService fileRecordService = mock(FileRecordService.class);
        OutboxService outboxService = mock(OutboxService.class);
        FileStorageUtil fileStorageUtil = mock(FileStorageUtil.class);

        FileUploadOrchestratorService service = new FileUploadOrchestratorService(
                testUploadProperties(),
                fileRecordService,
                outboxService,
                fileStorageUtil,
                List.of(),
                mock(FileContextMessageFactory.class)
        );

        byte[] sameContent = "same-content".getBytes();
        // Intentionally send .1 first, .0 second — order must NOT matter
        MockMultipartFile revision1 = new MockMultipartFile("files", "F5D_0031_0894_20250311.1", "application/octet-stream", sameContent);
        MockMultipartFile revision0 = new MockMultipartFile("files", "F5D_0031_0894_20250311.0", "application/octet-stream", sameContent);

        when(fileStorageUtil.saveInDiskOverridingExisting(any(Path.class), any(FileContext.class)))
                .thenAnswer(invocation -> FileHandlingResult.initial(invocation.getArgument(1)).withStoredInDisk());

        when(fileRecordService.saveNew(any(FileHandlingResult.class), eq(FileOrigin.API)))
                .thenAnswer(invocation -> ((FileHandlingResult) invocation.getArgument(0)).withPersistedInFileRecords());

        when(outboxService.saveDuplicated(any(FileHandlingResult.class), eq(InternalServices.CHAIN_VALIDATION), eq(FileOrigin.API)))
                .thenAnswer(invocation -> ((FileHandlingResult) invocation.getArgument(0)).withPersistedInOutboxEvent());

        // Send .1 first intentionally
        FileBatchResult result = service.processFiles(new MultipartFile[]{revision1, revision0});

        assertEquals(1, result.validFiles().size());
        assertEquals(1, result.duplicatedFiles().size());

        // .0 must be the accepted file
        assertEquals("F5D_0031_0894_20250311.0", result.validFiles().get(0).validationContext().getOriginalFilename());
        // .1 must be the duplicate
        assertEquals("F5D_0031_0894_20250311.1", result.duplicatedFiles().get(0).validationContext().getOriginalFilename());
    }

    private FileUploadProperties testUploadProperties() {
        String basePath = "/tmp/c4e-test";
        return new FileUploadProperties(
                basePath,
                basePath + "/pending",
                basePath + "/processed",
                basePath + "/processing",
                basePath + "/duplicates",
                basePath + "/failed",
                basePath + "/rejected",
                basePath + "/archive",
                basePath + "/automatic",
                1024 * 1024,
                List.of("xml"),
                List.of("application/xml")
        );
    }
}

