package com.com4energy.processor.util;

import com.com4energy.processor.config.properties.FileUploadProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileStorageUtilMoveTest {

    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM");

    @TempDir
    Path tempDir;

    @Test
    void moveFileToPendingAvoidsOverwritingExistingFile() throws IOException {
        FileStorageUtil storageUtil = new FileStorageUtil(uploadProperties());
        Path sourceDir = Files.createDirectories(tempDir.resolve("source"));
        Path sourceFile = Files.writeString(sourceDir.resolve("P1AA_TEST_20260101.0"), "new-content");

        Path pendingDir = Path.of(uploadProperties().pendingPath());
        Path typeAndDateDir = pendingDir.resolve("medida_h").resolve(LocalDate.now().format(YEAR_MONTH_FORMATTER));
        Files.createDirectories(typeAndDateDir);
        Path existingFile = Files.writeString(typeAndDateDir.resolve("P1AA_TEST_20260101.0"), "existing-content");

        Path moved = storageUtil.moveFileToPending(sourceFile.toFile());

        assertTrue(Files.exists(existingFile));
        assertEquals("existing-content", Files.readString(existingFile));
        assertTrue(Files.exists(moved));
        assertNotEquals(existingFile, moved);
        assertEquals("new-content", Files.readString(moved));
        assertTrue(moved.toString().contains("pending/medida_h"), moved.toString());
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

