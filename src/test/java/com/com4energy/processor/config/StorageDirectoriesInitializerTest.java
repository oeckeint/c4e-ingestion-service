package com.com4energy.processor.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.com4energy.processor.config.properties.FileScannerProperties;
import com.com4energy.processor.config.properties.FileUploadProperties;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StorageDirectoriesInitializerTest {

    @TempDir
    Path tempDir;

    @Test
    void runCreatesConfiguredDirectories() {
        FileUploadProperties upload = getUploadProperties();

        FileScannerProperties scanner = new FileScannerProperties();
        scanner.setPaths(List.of(tempDir.resolve("scanner/incoming").toString()));

        StorageDirectoriesInitializer initializer = new StorageDirectoriesInitializer(upload, scanner);

        assertDoesNotThrow(() -> initializer.run(null));

        assertTrue(Files.isDirectory(Path.of(upload.basePath())));
        assertTrue(Files.isDirectory(Path.of(upload.pendingPath())));
        assertTrue(Files.isDirectory(Path.of(upload.processedPath())));
        assertTrue(Files.isDirectory(Path.of(upload.processingPath())));
        assertTrue(Files.isDirectory(Path.of(upload.duplicatesPath())));
        assertTrue(Files.isDirectory(Path.of(upload.failedPath())));
        assertTrue(Files.isDirectory(Path.of(upload.rejectedPath())));
        assertTrue(Files.isDirectory(Path.of(upload.archivePath())));
        assertTrue(Files.isDirectory(Path.of(upload.automaticPath())));
        assertTrue(Files.isDirectory(Path.of(scanner.getPaths().get(0))));
    }

    private @NotNull FileUploadProperties getUploadProperties() {
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
                List.of("csv", "xls", "xlsx", "json", "txt"),
                List.of("text/xml", "application/xml")
        );
    }

    @Test
    void runFailsWhenRequiredPathIsBlank() {
        FileUploadProperties upload = new FileUploadProperties(
                " ",
                tempDir.resolve("pending").toString(),
                tempDir.resolve("processed").toString(),
                tempDir.resolve("processing").toString(),
                tempDir.resolve("duplicates").toString(),
                tempDir.resolve("failed").toString(),
                tempDir.resolve("rejected").toString(),
                tempDir.resolve("archive").toString(),
                tempDir.resolve("automatic").toString(),
                10L * 1024 * 1024,
                List.of("csv"),
                List.of("text/xml")
        );

        FileScannerProperties scanner = new FileScannerProperties();
        scanner.setPaths(List.of(tempDir.resolve("scanner/incoming").toString()));

        StorageDirectoriesInitializer initializer = new StorageDirectoriesInitializer(upload, scanner);

        assertThrows(IllegalStateException.class, () -> initializer.run(null));
    }

}
