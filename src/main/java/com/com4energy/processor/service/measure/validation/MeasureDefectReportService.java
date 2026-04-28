package com.com4energy.processor.service.measure.validation;

import com.com4energy.processor.config.properties.FileUploadProperties;
import com.com4energy.processor.service.measure.MeasureLineParseError;
import com.com4energy.processor.service.measure.MeasureRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeasureDefectReportService {

    private static final String DEFECT_BASE_SUFFIX = ".sge_defect";
    private static final String QUARANTINE_BASE_SUFFIX = ".sge_quarantine";
    private static final String JSONL_EXTENSION = ".jsonl";
    private static final String CSV_EXTENSION = ".csv";
        private static final String CSV_HEADER = "originalFile,phase,line,brokenRule,message,rawLine";

    private final FileUploadProperties fileUploadProperties;

    public Optional<Path> writeParseDefectReport(String originalFilename, List<MeasureLineParseError> parseErrors) {
        List<DefectEntry> entries = new ArrayList<>();
        for (MeasureLineParseError error : parseErrors) {
            entries.add(new DefectEntry(
                    originalFilename,
                    "parse",
                    error.lineNumber(),
                    "PARSE_FORMAT",
                    error.message(),
                    error.rawLine()
            ));
        }
        return write(originalFilename, entries);
    }

    public Optional<Path> writeValidationAndPersistenceDefectReport(
            String originalFilename,
            List<MeasureRecordValidationError> validationErrors,
            List<String> persistenceErrors
    ) {
        List<DefectEntry> entries = new ArrayList<>();
        for (MeasureRecordValidationError error : validationErrors) {
            entries.add(new DefectEntry(
                    originalFilename,
                    "validation",
                    error.recordIndex(),
                    error.brokenRule(),
                    error.message(),
                    error.rawLine()
            ));
        }
        for (String error : persistenceErrors) {
            entries.add(new DefectEntry(
                    originalFilename,
                    "persistence",
                    null,
                    "PERSISTENCE",
                    error,
                    null
            ));
        }
        return write(originalFilename, entries);
    }

    /**
     * Escribe registros que fallaron en persistencia (binary split quarantine).
     * Archivo separado de los defectos de validación: usa sufijo .sge_quarantine.jsonl
     * Solo JSONL (sin CSV) porque está destinado a retry automático, no a revisión humana.
     *
     * @param originalFilename nombre del archivo original
     * @param failedRecords registros que fallaron con detalles de error
     * @return path al archivo .sge_quarantine.jsonl
     */
    public Optional<Path> writeQuarantineDefectReport(
            String originalFilename,
            List<PersistenceFailedRecord> failedRecords
    ) {
        if (failedRecords.isEmpty()) {
            return Optional.empty();
        }

        List<DefectEntry> entries = new ArrayList<>();
        int recordIndex = 0;
        for (PersistenceFailedRecord failedRecord : failedRecords) {
            entries.add(new DefectEntry(
                    originalFilename,
                    "persistence_quarantine",
                    recordIndex,
                    failedRecord.entityType(),
                    failedRecord.errorMessage(),
                    failedRecord.recordString()
            ));
            recordIndex++;
        }

        if (entries.isEmpty()) {
            return Optional.empty();
        }

        try {
            Path defectsDir = Paths.get(fileUploadProperties.failedPath(), "defects").toAbsolutePath().normalize();
            Files.createDirectories(defectsDir);
            String baseFilename = safeBaseName(originalFilename) + QUARANTINE_BASE_SUFFIX;
            Path targetBasePath = resolveTargetBasePath(defectsDir, baseFilename);
            // Solo JSONL para cuarentena — es para retry, no para revisión humana
            Path jsonlPath = toFormatPath(targetBasePath, JSONL_EXTENSION);
            Files.write(jsonlPath, toJsonLines(entries), java.nio.charset.StandardCharsets.UTF_8);
            log.info("Quarantine report written: {} ({} records)", jsonlPath, entries.size());
            return Optional.of(jsonlPath);
        } catch (IOException e) {
            log.warn("Could not write quarantine report for file '{}': {}", originalFilename, e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<Path> write(String originalFilename, List<DefectEntry> entries) {
        if (entries.isEmpty()) {
            return Optional.empty();
        }

        try {
            Path defectsDir = Paths.get(fileUploadProperties.failedPath(), "defects").toAbsolutePath().normalize();
            Files.createDirectories(defectsDir);
            String baseFilename = safeBaseName(originalFilename) + DEFECT_BASE_SUFFIX;
            Path targetBasePath = resolveTargetBasePath(defectsDir, baseFilename);
            Path jsonlPath = toFormatPath(targetBasePath, JSONL_EXTENSION);
            Path csvPath = toFormatPath(targetBasePath, CSV_EXTENSION);

            Files.write(jsonlPath, toJsonLines(entries), StandardCharsets.UTF_8);
            Files.write(csvPath, toCsvLines(entries), StandardCharsets.UTF_8);
            return Optional.of(jsonlPath);
        } catch (IOException e) {
            log.warn("Could not write defects report for file '{}': {}", originalFilename, e.getMessage());
            return Optional.empty();
        }
    }

    private Path resolveTargetBasePath(Path defectsDir, String filenameBase) {
        Path candidate = defectsDir.resolve(filenameBase).normalize();
        Path candidateJsonl = toFormatPath(candidate, JSONL_EXTENSION);
        Path candidateCsv = toFormatPath(candidate, CSV_EXTENSION);
        if (!Files.exists(candidateJsonl) && !Files.exists(candidateCsv)) {
            return candidate;
        }

        String withTimestamp = filenameBase + "." + Instant.now().toEpochMilli();
        return defectsDir.resolve(withTimestamp).normalize();
    }

    private Path toFormatPath(Path basePath, String extension) {
        String filename = basePath.getFileName().toString() + extension;
        return basePath.resolveSibling(filename).normalize();
    }

    private String safeBaseName(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "unknown";
        }
        return originalFilename.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private List<String> toJsonLines(List<DefectEntry> entries) {
        List<String> lines = new ArrayList<>(entries.size());
        for (DefectEntry entry : entries) {
            lines.add(toJsonLine(entry));
        }
        return lines;
    }

    private String toJsonLine(DefectEntry entry) {
        return "{" +
                "\"originalFile\":\"" + escape(entry.originalFilename()) + "\"," +
                "\"phase\":\"" + escape(entry.phase()) + "\"," +
                "\"line\":" + (entry.lineNumber() == null ? "null" : entry.lineNumber()) + "," +
                "\"brokenRule\":\"" + escape(entry.brokenRule()) + "\"," +
                "\"message\":\"" + escape(entry.message()) + "\"," +
                "\"rawLine\":" + (entry.rawLine() == null ? "null" : ("\"" + escape(entry.rawLine()) + "\"")) +
                "}";
    }

    private List<String> toCsvLines(List<DefectEntry> entries) {
        List<String> lines = new ArrayList<>(entries.size() + 1);
        lines.add(CSV_HEADER);
        for (DefectEntry entry : entries) {
            lines.add(toCsvLine(entry));
        }
        return lines;
    }

    private String toCsvLine(DefectEntry entry) {
        return csv(entry.originalFilename()) + ","
                + csv(entry.phase()) + ","
                + (entry.lineNumber() == null ? "" : entry.lineNumber()) + ","
                + csv(entry.brokenRule()) + ","
                + csv(entry.message()) + ","
                + csv(entry.rawLine());
    }

    private String csv(String value) {
        if (value == null) {
            return "";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private record DefectEntry(
            String originalFilename,
            String phase,
            Integer lineNumber,
            String brokenRule,
            String message,
            String rawLine
    ) {
    }

    /**
     * Registro para persistencia de medidas que fallaron en el binary split.
     */
    public record PersistenceFailedRecord(
            MeasureRecord record,
            String entityType,
            String errorMessage,
            String recordString
    ) {
    }
}
