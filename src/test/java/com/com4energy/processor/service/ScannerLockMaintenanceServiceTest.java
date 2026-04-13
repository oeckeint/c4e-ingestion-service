package com.com4energy.processor.service;

import com.com4energy.processor.config.properties.FileScannerProperties;
import com.com4energy.processor.config.properties.FileUploadProperties;
import com.com4energy.processor.util.FileStorageUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;

import com.com4energy.processor.outbox.service.OutboxService;
import com.com4energy.processor.config.InstanceIdentifier;
import com.com4energy.processor.service.FileRecordService;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScannerLockMaintenanceServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void cleanupMovesExpiredLockFileToFailedFolder() throws IOException {
        Path lockDir = tempDir.resolve("scanner-lock");
        Files.createDirectories(lockDir);
        Path staleFile = Files.writeString(lockDir.resolve("stale.xml"), "<xml/>");
        Files.setLastModifiedTime(staleFile, FileTime.fromMillis(System.currentTimeMillis() - 10_000));

        ScannerLockMaintenanceService service = buildService(lockDir, 1_000);

        service.cleanupExpiredLocks();

        assertFalse(Files.exists(staleFile));
        assertTrue(Files.exists(tempDir.resolve("failed/stale.xml")));
    }

    @Test
    void cleanupKeepsRecentLockFile() throws IOException {
        Path lockDir = tempDir.resolve("scanner-lock");
        Files.createDirectories(lockDir);
        Path recentFile = Files.writeString(lockDir.resolve("recent.xml"), "<xml/>");

        ScannerLockMaintenanceService service = buildService(lockDir, 60_000);

        service.cleanupExpiredLocks();

        assertTrue(Files.exists(recentFile));
        assertFalse(Files.exists(tempDir.resolve("failed/recent.xml")));
    }

    private ScannerLockMaintenanceService buildService(Path lockDir, long maxAgeMs) {
        FileScannerProperties scannerProperties = new FileScannerProperties();
        scannerProperties.setPaths(List.of(tempDir.resolve("automatic").toString()));
        scannerProperties.setLockPath(lockDir.toString());
        scannerProperties.setLockMaxAgeMs(maxAgeMs);
        scannerProperties.setLockMaintenanceIntervalMs(5_000);
        scannerProperties.setScanIntervalMs(5_000);

        FileUploadProperties uploadProperties = new FileUploadProperties(
                tempDir.toString(),
                tempDir.resolve("pending").toString(),
                tempDir.resolve("processed").toString(),
                tempDir.resolve("processing").toString(),
                tempDir.resolve("duplicates").toString(),
                tempDir.resolve("failed").toString(),
                tempDir.resolve("rejected").toString(),
                tempDir.resolve("archive").toString(),
                tempDir.resolve("automatic").toString(),
                10L * 1024 * 1024,
                List.of("xml"),
                List.of("application/xml")
        );

        FileStorageUtil storageUtil = new FileStorageUtil(uploadProperties);
        OutboxService outboxService = null;
        FileRecordService fileRecordService = null;
        InstanceIdentifier instanceIdentifier = new InstanceIdentifier();
        return new ScannerLockMaintenanceService(scannerProperties, storageUtil, outboxService, fileRecordService, instanceIdentifier);
    }
}



