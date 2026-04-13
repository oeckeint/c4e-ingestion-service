package com.com4energy.processor.util;

import com.com4energy.processor.config.properties.FileUploadProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileStorageUtilMoveTest {

    @TempDir
    Path tempDir;

    @Test
    void moveFileToPendingAvoidsOverwritingExistingFile() throws IOException {
        FileStorageUtil storageUtil = new FileStorageUtil(uploadProperties());
        Path sourceDir = Files.createDirectories(tempDir.resolve("source"));
        Path sourceFile = Files.writeString(sourceDir.resolve("measure.0"), "new-content");

        Path pendingDir = Path.of(uploadProperties().pendingPath());
        Files.createDirectories(pendingDir);
        Path existingFile = Files.writeString(pendingDir.resolve("measure.0"), "existing-content");

        Path moved = storageUtil.moveFileToPending(sourceFile.toFile());

        assertTrue(Files.exists(existingFile));
        assertEquals("existing-content", Files.readString(existingFile));
        assertTrue(Files.exists(moved));
        assertNotEquals(existingFile, moved);
        assertEquals("new-content", Files.readString(moved));
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
                List.of("xml", "0"),
                List.of("application/xml", "text/plain", "application/octet-stream")
        );
    }
}

