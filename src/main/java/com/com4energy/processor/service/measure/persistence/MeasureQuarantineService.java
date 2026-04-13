package com.com4energy.processor.service.measure.persistence;

import com.com4energy.processor.service.measure.MeasureRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestionar registros de medidas que fallaron en persistencia.
 * Escribe los registros malos en archivos JSONL en cuarentena para posterior análisis y retry.
 */
@Slf4j
@Service
public class MeasureQuarantineService {

    private static final String QUARANTINE_DIR = "quarantine";
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private final ObjectMapper objectMapper;
    private final String quarantineBasePath;

    public MeasureQuarantineService(
            ObjectMapper objectMapper,
            @Value("${app.quarantine-path:${java.io.tmpdir}/c4e-quarantine}") String quarantineBasePath
    ) {
        this.objectMapper = objectMapper;
        this.quarantineBasePath = quarantineBasePath;
    }

    /**
     * Escribe registros fallidos en archivo JSONL de cuarentena.
     *
     * @param fileRecordId ID del archivo original
     * @param failedRecords registros que fallaron
     * @param errorMessage mensaje de error
     * @return path al archivo de cuarentena
     */
    public String quarantineFailedRecords(
            Long fileRecordId,
            List<MeasureRecord> failedRecords,
            String errorMessage
    ) {
        if (failedRecords.isEmpty()) {
            return null;
        }

        try {
            Path quarantinePath = createQuarantineDirectory();
            String filename = generateQuarantineFilename(fileRecordId);
            Path filePath = quarantinePath.resolve(filename);

            writeJsonlRecords(filePath, failedRecords, errorMessage);

            log.info(
                    "Quarantined {} failed records for file {} to {}",
                    failedRecords.size(),
                    fileRecordId,
                    filePath.toAbsolutePath()
            );

            return filePath.toAbsolutePath().toString();

        } catch (IOException e) {
            log.error("Error creating quarantine file for file {}", fileRecordId, e);
            return null;
        }
    }

    private Path createQuarantineDirectory() throws IOException {
        Path path = Paths.get(quarantineBasePath);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
        return path;
    }

    private String generateQuarantineFilename(Long fileRecordId) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP);
        return String.format("quarantine_file_%d_%s.jsonl", fileRecordId, timestamp);
    }

    private void writeJsonlRecords(Path path, List<MeasureRecord> records, String errorMessage) throws IOException {
        String jsonlContent = records.stream()
                .map(record -> {
                    try {
                        return objectMapper.writeValueAsString(
                                new QuarantineRecord(record, errorMessage)
                        );
                    } catch (Exception e) {
                        log.warn("Error serializing record to JSON", e);
                        return null;
                    }
                })
                .filter(line -> line != null)
                .collect(Collectors.joining("\n"));

        Files.writeString(path, jsonlContent);
    }

    /**
     * Wrapper para serializar registros de medidas con contexto de error.
     */
    public record QuarantineRecord(
            MeasureRecord record,
            String errorMessage,
            LocalDateTime quarantinedAt
    ) {
        public QuarantineRecord(MeasureRecord record, String errorMessage) {
            this(record, errorMessage, LocalDateTime.now());
        }
    }
}

