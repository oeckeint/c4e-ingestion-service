package com.com4energy.processor.service;

import com.com4energy.processor.config.properties.FileScannerProperties;
import com.com4energy.processor.common.InternalServices;
import com.com4energy.processor.config.InstanceIdentifier;
import com.com4energy.processor.model.FailureReason;
import com.com4energy.processor.model.FileRecord;
import com.com4energy.processor.model.FileOrigin;
import com.com4energy.processor.outbox.service.OutboxService;
import com.com4energy.processor.service.dto.FileContext;
import com.com4energy.processor.service.dto.FileHandlingResult;
import com.com4energy.processor.service.dto.PathMultipartFile;
import com.com4energy.processor.service.validation.ValidationContext;
import com.com4energy.processor.util.FileStorageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScannerLockMaintenanceService {

    private final FileScannerProperties fileScannerProperties;
    private final FileStorageUtil fileStorageUtil;
    private final OutboxService outboxService;
    private final FileRecordService fileRecordService;
    private final InstanceIdentifier instanceIdentifier;

    public void cleanupExpiredLocks() {
        Path lockDirectory = Paths.get(fileScannerProperties.getLockPath());
        if (!Files.isDirectory(lockDirectory)) {
            log.warn("Scanner lock directory is not available: {}", lockDirectory);
            return;
        }

        long now = System.currentTimeMillis();
        long lockMaxAgeMs = fileScannerProperties.getLockMaxAgeMs();
        long movedToFailed = 0;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(lockDirectory)) {
            for (Path lockFile : stream) {
                if (!Files.isRegularFile(lockFile) || !isExpired(lockFile, now, lockMaxAgeMs)) {
                    continue;
                }

                Path movedPath = fileStorageUtil.moveFileToFailed(lockFile.toFile());
                if (movedPath != null) {
                    movedToFailed++;
                    log.warn("Moved stale scanner lock file to failed folder: {} -> {}", lockFile, movedPath);
                    publishRejectedEvent(movedPath);
                } else {
                    log.error("Could not move stale scanner lock file to failed folder: {}", lockFile);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Error while cleaning scanner lock directory: " + lockDirectory, e);
        }

        if (movedToFailed > 0) {
            log.info("Scanner lock maintenance moved {} stale files to failed folder", movedToFailed);
        }

        if (fileRecordService != null) {
            cleanupDatabaseLocksExpirado();
        }
    }

    private boolean isExpired(Path file, long now, long lockMaxAgeMs) {
        try {
            long lastModifiedMs = Files.getLastModifiedTime(file).toMillis();
            return now - lastModifiedMs >= lockMaxAgeMs;
        } catch (IOException e) {
            log.warn("Could not inspect last modified time for lock file '{}': {}", file, e.getMessage());
            return false;
        }
    }

    private void publishRejectedEvent(Path failedFilePath) {
        try {
            MultipartFile multipartFile = PathMultipartFile.fromPath(failedFilePath);
            ValidationContext validationContext = ValidationContext.from(multipartFile);
            FileContext fileContext = FileContext.fromWithCriticalValidationFailedStatus(
                    validationContext,
                    List.of(FailureReason.STALE_LOCK),
                    FailureReason.STALE_LOCK
            );
            FileHandlingResult result = FileHandlingResult.initial(fileContext).withStoredInDisk();
            outboxService.saveRejected(result, InternalServices.SCANNER_LOCK_MAINTENANCE, FileOrigin.JOB);
        } catch (Exception e) {
            log.error("Could not publish rejected event for stale lock file '{}': {}", failedFilePath, e.getMessage());
        }
    }

    private void cleanupDatabaseLocksExpirado() {
        int lockTtlMinutes = (int) (fileScannerProperties.getLockMaxAgeMs() / (1000 * 60));
        List<FileRecord> expiredLocks = fileRecordService.findLockedFilesOlderThan(lockTtlMinutes);

        for (FileRecord expiredLock : expiredLocks) {
            log.warn("Detected expired lock on FileRecord: id={}, lockedBy={}, lockedAt={}",
                    expiredLock.getId(), expiredLock.getLockedBy(), expiredLock.getLockedAt());
            fileRecordService.releaseLock(expiredLock);
        }

        if (!expiredLocks.isEmpty()) {
            log.info("Scanner lock maintenance released {} expired database locks", expiredLocks.size());
        }
    }
}
