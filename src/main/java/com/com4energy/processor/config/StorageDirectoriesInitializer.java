package com.com4energy.processor.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import com.com4energy.processor.config.properties.FileScannerProperties;
import com.com4energy.processor.config.properties.FileUploadProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StorageDirectoriesInitializer implements ApplicationRunner {

    private final FileUploadProperties fileUploadProperties;
    private final FileScannerProperties fileScannerProperties;

    @Override
    public void run(ApplicationArguments args) {
        ensureDirectory("c4e.upload.base-path", fileUploadProperties.basePath());
        ensureDirectory("c4e.upload.pending-path", fileUploadProperties.pendingPath());
        ensureDirectory("c4e.upload.processed-path", fileUploadProperties.processedPath());
        ensureDirectory("c4e.upload.processing-path", fileUploadProperties.processingPath());
        ensureDirectory("c4e.upload.duplicates-path", fileUploadProperties.duplicatesPath());
        ensureDirectory("c4e.upload.failed-path", fileUploadProperties.failedPath());
        ensureDirectory("c4e.upload.rejected-path", fileUploadProperties.rejectedPath());
        ensureDirectory("c4e.upload.archive-path", fileUploadProperties.archivePath());
        ensureDirectory("c4e.upload.automatic-path", fileUploadProperties.automaticPath());

        List<String> scannerPaths = fileScannerProperties.getPaths();
        if (scannerPaths != null) {
            for (int i = 0; i < scannerPaths.size(); i++) {
                ensureDirectory("scanner.paths[" + i + "]", scannerPaths.get(i));
            }
        }

        log.info("Storage directories are ready under base path: {}", fileUploadProperties.basePath());
    }

    private void ensureDirectory(String propertyName, String directoryPath) {
        if (directoryPath == null || directoryPath.isBlank()) {
            throw new IllegalStateException("Property " + propertyName + " must not be empty");
        }

        Path path = Paths.get(directoryPath);
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create directory for " + propertyName + ": " + directoryPath, e);
        }

        if (!Files.isDirectory(path)) {
            throw new IllegalStateException("Path for " + propertyName + " is not a directory: " + directoryPath);
        }
        if (!Files.isWritable(path)) {
            throw new IllegalStateException("Path for " + propertyName + " is not writable: " + directoryPath);
        }
    }
}

