package com.com4energy.processor.service.measure.validation;

import com.com4energy.processor.config.properties.FileUploadProperties;
import com.com4energy.processor.service.measure.MeasureLineParseError;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MeasureDefectReportServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void writeParseDefectReportCreatesJsonlAndCsv() throws IOException {
        MeasureDefectReportService service = new MeasureDefectReportService(properties());

        Optional<Path> reportPath = service.writeParseDefectReport(
                "P1D_0021_0894_20240104.0",
                List.of(new MeasureLineParseError(7, "bad;line", "Formato inválido"))
        );

        assertTrue(reportPath.isPresent());
        Path jsonl = reportPath.get();
        assertTrue(jsonl.getFileName().toString().endsWith(".sge_defect.jsonl"));
        Path csv = jsonl.resolveSibling(jsonl.getFileName().toString().replace(".jsonl", ".csv"));
        assertTrue(Files.exists(jsonl));
        assertTrue(Files.exists(csv));

        List<String> csvLines = Files.readAllLines(csv);
            assertEquals("originalFile,phase,line,brokenRule,message,rawLine", csvLines.get(0));
        assertTrue(csvLines.get(1).contains("\"parse\""));
        assertTrue(csvLines.get(1).contains("7"));
    }

    @Test
    void writeValidationAndPersistenceDefectReportCreatesCombinedEntries() throws IOException {
        MeasureDefectReportService service = new MeasureDefectReportService(properties());

        Optional<Path> reportPath = service.writeValidationAndPersistenceDefectReport(
                "F5D_0031_0894_20250311.0",
                List.of(new MeasureRecordValidationError(3, "MANDATORY_FIELDS", "CUPS vacío", "ES001;;;")),
                List.of("No se encontró cliente para CUPS ES0001")
        );

        assertTrue(reportPath.isPresent());
        List<String> jsonLines = Files.readAllLines(reportPath.get());
        assertEquals(2, jsonLines.size());
        assertTrue(jsonLines.get(0).contains("\"phase\":\"validation\""));
        assertTrue(jsonLines.get(0).contains("\"rawLine\":\"ES001;;;\""));
        assertTrue(jsonLines.get(1).contains("\"phase\":\"persistence\""));
    }

    @Test
    void writeReturnsEmptyWhenNoEntries() {
        MeasureDefectReportService service = new MeasureDefectReportService(properties());

        Optional<Path> parsePath = service.writeParseDefectReport("file.0", List.of());
        Optional<Path> validationPath = service.writeValidationAndPersistenceDefectReport("file.0", List.of(), List.of());

        assertTrue(parsePath.isEmpty());
        assertTrue(validationPath.isEmpty());
    }

    private FileUploadProperties properties() {
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
                List.of("xml", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9"),
                List.of("application/xml", "text/plain", "application/octet-stream")
        );
    }
}

