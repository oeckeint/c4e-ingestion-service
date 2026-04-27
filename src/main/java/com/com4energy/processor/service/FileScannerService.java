package com.com4energy.processor.service;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import com.com4energy.i18n.core.Messages;
import com.com4energy.processor.common.LogsCommonMessageKey;
import com.com4energy.processor.exception.FileScannerException;
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

    private final FileUploadOrchestratorService fileUploadOrchestratorService;
    private final FileStorageUtil fileStorageUtil;
    private final FileScannerProperties fileScannerProperties;

    private enum FileProcessResult {
        PROCESSED, SKIPPED
    }

    public void scanAndRegisterFiles() {
        Path lockDirectory = Paths.get(fileScannerProperties.getLockPath());
        try {
            Files.createDirectories(lockDirectory);

            for (String pathStr : fileScannerProperties.getPaths()) {
                Path path = Paths.get(pathStr);
                if (Files.exists(path) && Files.isDirectory(path)) {
                    long filesFound = 0;
                    try (DirectoryStream<Path> filesFromFolder = Files.newDirectoryStream(path)) {
                        for (Path file : filesFromFolder) {
                            if (processFileFromFolder(file, lockDirectory) == FileProcessResult.PROCESSED) {
                                filesFound++;
                            }
                        }
                        if (filesFound > 0) {
                            log.info(Messages.format(LogsCommonMessageKey.FILE_FOUND, filesFound, pathStr));
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new FileScannerException(Messages.format(LogsCommonMessageKey.SCANNER_DIRECTORY_SCAN_ERROR), e);
        }
    }

    /**
     * Procesa un archivo del folder, intentando reclamarlo y procesarlo si es regular.
     * @param file archivo a procesar
     * @param lockDirectory directorio de lock
     * @return FileProcessResult indicando si fue procesado o no
     */
    private FileProcessResult processFileFromFolder(Path file, Path lockDirectory) {
        if (Files.isRegularFile(file)) {
            Path lockedFile = claimFile(file, lockDirectory);
            if (lockedFile != null) {
                processClaimedFile(lockedFile);
                return FileProcessResult.PROCESSED;
            }
        }

        return FileProcessResult.SKIPPED;
    }

    private Path claimFile(Path sourceFile, Path lockDirectory) {
        Path targetFile = lockDirectory.resolve(sourceFile.getFileName());
        try {
            return Files.move(sourceFile, targetFile, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            try {
                return Files.move(sourceFile, targetFile);
            } catch (FileAlreadyExistsException alreadyClaimed) {
                log.debug(Messages.format(LogsCommonMessageKey.FILE_ALREADY_CLAIMED, targetFile));
                return null;
            } catch (IOException fallbackException) {
                log.debug(Messages.format(LogsCommonMessageKey.FILE_COULD_NOT_CLAIM, sourceFile, lockDirectory, fallbackException.getMessage()));
                return null;
            }
        } catch (FileAlreadyExistsException e) {
            log.debug(Messages.format(LogsCommonMessageKey.FILE_ALREADY_CLAIMED, targetFile));
            return null;
        } catch (IOException e) {
            log.debug(Messages.format(LogsCommonMessageKey.FILE_COULD_NOT_CLAIM, sourceFile, lockDirectory, e.getMessage()));
            return null;
        }
    }

    private void processClaimedFile(Path lockedFile) {
        try {
            MultipartFile multipartFile = PathMultipartFile.fromPath(lockedFile);
            FileBatchResult batchResult = fileUploadOrchestratorService.processFiles(new MultipartFile[]{multipartFile}, FileOrigin.JOB);
            log.info(Messages.format(LogsCommonMessageKey.SCANNER_CLASSIFIED_FILE,
                    lockedFile.getFileName(), batchResult.processed(), batchResult.successCount(), batchResult.errors(), batchResult.alreadyExistsCount()));
            if (!fileStorageUtil.deleteIfExists(lockedFile)) {
                log.warn(Messages.format(LogsCommonMessageKey.COULD_NOT_DELETE_CLAIMED_FILE, lockedFile));
            }
        } catch (Exception e) {
            log.error(Messages.format(LogsCommonMessageKey.ERROR_CLASSIFYING_CLAIMED_FILE, lockedFile), e);
        }
    }

}
