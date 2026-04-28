package com.com4energy.processor.service;

import com.com4energy.processor.config.properties.FileScannerProperties;
import com.com4energy.processor.model.FileOrigin;
import com.com4energy.processor.service.dto.FileBatchResult;
import com.com4energy.processor.util.FileStorageUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FileScannerServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void scanAndRegisterFilesClaimsProcessesAndDeletesLockedFile() throws IOException {
        Path automaticDir = Files.createDirectories(tempDir.resolve("automatic"));
        Path lockDir = tempDir.resolve(".scanner-lock");
        Path incomingFile = Files.writeString(automaticDir.resolve("batch.xml"), "<xml/>");

        FileUploadOrchestratorService orchestratorService = mock(FileUploadOrchestratorService.class);
        FileStorageUtil fileStorageUtil = mock(FileStorageUtil.class);

        when(orchestratorService.processFiles(any(), eq(FileOrigin.JOB))).thenReturn(FileBatchResult.empty());
        when(fileStorageUtil.deleteIfExists(any(Path.class))).thenReturn(true);

        FileScannerService service = new FileScannerService(
                orchestratorService,
                fileStorageUtil,
                scannerProperties(automaticDir, lockDir)
        );

        service.scanAndRegisterFiles();

        verify(orchestratorService).processFiles(any(), eq(FileOrigin.JOB));
        verify(fileStorageUtil).deleteIfExists(argThat(path -> path != null && path.endsWith("batch.xml")));
        assertFalse(Files.exists(incomingFile));
        assertTrue(Files.isDirectory(lockDir));
    }

    @Test
    void scanAndRegisterFilesLeavesClaimedFileInLockFolderWhenClassificationFails() throws IOException {
        Path automaticDir = Files.createDirectories(tempDir.resolve("automatic"));
        Path lockDir = tempDir.resolve(".scanner-lock");
        Path incomingFile = Files.writeString(automaticDir.resolve("failing.xml"), "<xml/>");
        Path lockedFile = lockDir.resolve("failing.xml");

        FileUploadOrchestratorService orchestratorService = mock(FileUploadOrchestratorService.class);
        FileStorageUtil fileStorageUtil = mock(FileStorageUtil.class);

        when(orchestratorService.processFiles(any(), eq(FileOrigin.JOB))).thenThrow(new RuntimeException("boom"));

        FileScannerService service = new FileScannerService(
                orchestratorService,
                fileStorageUtil,
                scannerProperties(automaticDir, lockDir)
        );

        service.scanAndRegisterFiles();

        assertFalse(Files.exists(incomingFile));
        assertTrue(Files.exists(lockedFile));
        verify(fileStorageUtil, never()).deleteIfExists(any(Path.class));
    }

    private FileScannerProperties scannerProperties(Path automaticDir, Path lockDir) {
        FileScannerProperties props = new FileScannerProperties();
        props.setPaths(List.of(automaticDir.toString()));
        props.setLockPath(lockDir.toString());
        props.setScanIntervalMs(5_000);
        props.setLockMaxAgeMs(60_000);
        props.setLockMaintenanceIntervalMs(5_000);
        return props;
    }
}




