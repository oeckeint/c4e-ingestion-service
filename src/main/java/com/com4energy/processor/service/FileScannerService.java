package com.com4energy.processor.service;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import com.com4energy.processor.config.FeatureFlagService;
import com.com4energy.processor.model.FileOrigin;
import com.com4energy.processor.service.dto.FileBatchResult;
import com.com4energy.processor.service.dto.PathMultipartFile;
import com.com4energy.processor.util.FileStorageUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.com4energy.processor.config.properties.FileScannerProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileScannerService {

    private final FeatureFlagService featureFlagService;
    private final FileUploadOrchestratorService fileUploadOrchestratorService;
    private final FileStorageUtil fileStorageUtil;
    private final FileScannerProperties fileScannerProperties;

    public void scanAndRegisterFiles() {
        if (featureFlagService.isPersistenceEnabled()) {
            log.info("Persist records disabled by feature flag. Skipping scanner run");
            return;
        }

        Path lockDirectory = Paths.get(fileScannerProperties.getLockPath());
        ensureLockDirectoryExists(lockDirectory);

        for (String pathStr : fileScannerProperties.getPaths()) {
            Path path = Paths.get(pathStr);
            if (Files.exists(path) && Files.isDirectory(path)) {
                long filesFound = 0;
                try (DirectoryStream<Path> filesFromFolder = Files.newDirectoryStream(path)) {
                    for (Path file : filesFromFolder) {
                        if (Files.isRegularFile(file)) {
                            filesFound++;
                            Path lockedFile = claimFile(file, lockDirectory);
                            if (lockedFile == null) {
                                continue;
                            }
                            processClaimedFile(lockedFile);
                        }
                    }
                    if (filesFound > 0) {
                        log.info("{} files found in {}", filesFound, pathStr);
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Error scanning directory: " + pathStr, e);
                }
            }
        }
    }

    private void ensureLockDirectoryExists(Path lockDirectory) {
        try {
            Files.createDirectories(lockDirectory);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create scanner lock directory: " + lockDirectory, e);
        }
    }

    private Path claimFile(Path sourceFile, Path lockDirectory) {
        Path targetFile = lockDirectory.resolve(sourceFile.getFileName());
        try {
            return Files.move(sourceFile, targetFile, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            try {
                return Files.move(sourceFile, targetFile);
            } catch (FileAlreadyExistsException alreadyClaimed) {
                log.warn("File already claimed in lock directory during fallback move, skipping: {}", targetFile);
                return null;
            } catch (IOException fallbackException) {
                log.warn("Could not claim file '{}' into lock directory '{}': {}",
                        sourceFile,
                        lockDirectory,
                        fallbackException.getMessage());
                return null;
            }
        } catch (FileAlreadyExistsException e) {
            log.warn("File already claimed in lock directory, skipping: {}", targetFile);
            return null;
        } catch (IOException e) {
            log.warn("Could not claim file '{}' into lock directory '{}': {}", sourceFile, lockDirectory, e.getMessage());
            return null;
        }
    }

    private void processClaimedFile(Path lockedFile) {
        try {
            MultipartFile multipartFile = PathMultipartFile.fromPath(lockedFile);
            FileBatchResult batchResult = fileUploadOrchestratorService.processFiles(new MultipartFile[]{multipartFile}, FileOrigin.JOB);
            log.info("Scanner classified file '{}': processed={}, success={}, errors={}, duplicates={}",
                    lockedFile.getFileName(),
                    batchResult.processed(),
                    batchResult.successCount(),
                    batchResult.errors(),
                    batchResult.alreadyExistsCount());
            if (!fileStorageUtil.deleteIfExists(lockedFile)) {
                log.warn("Could not delete claimed lock file after classification: {}", lockedFile);
            }
        } catch (Exception e) {
            log.error("Error classifying claimed file '{}'. File remains in lock directory for manual review.", lockedFile, e);
        }
    }

}
