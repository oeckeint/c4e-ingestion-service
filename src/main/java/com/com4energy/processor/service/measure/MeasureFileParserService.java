package com.com4energy.processor.service.measure;

import com.com4energy.i18n.core.Messages;
import com.com4energy.processor.common.LogsCommonMessageKey;
import com.com4energy.processor.model.FileType;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class MeasureFileParserService {

    private static final int EXPECTED_P1_COLUMNS = 22;
    private static final int MIN_P2_COLUMNS = 21;
    private static final int MAX_P2_COLUMNS = 23;
    private static final int EXPECTED_F5_COLUMNS = 12;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
    private static final DateTimeFormatter DATE_TIME_WITH_SECONDS_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    /**
     * Parse a measure file using the provided FileType to determine the structure.
     * The FileType comes from the database (pre-classified at ingestion time) — not recalculated here.
     */
    public MeasureParseResult parse(Path file, FileType fileType) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return parse(file.getFileName().toString(), reader, fileType);
        }
    }

    /**
     * Parse a measure file using the provided FileType.
     * @param fileName the filename (used for error reporting only)
     * @param reader the file content reader
     * @param fileType the pre-classified file type from the database
     */
    public MeasureParseResult parse(String fileName, Reader reader, FileType fileType) throws IOException {
        MeasureFilenameMetadata metadata = new MeasureFilenameMetadata(fileName, fileType);
        List<MeasureRecord> records = new ArrayList<>();
        List<MeasureLineParseError> errors = new ArrayList<>();

        try (BufferedReader bufferedReader = toBufferedReader(reader)) {
            String line;
            int lineNumber = 1;
            while ((line = bufferedReader.readLine()) != null) {
                try {
                    records.add(parseLine(metadata, line, lineNumber));
                } catch (IllegalArgumentException ex) {
                    errors.add(new MeasureLineParseError(lineNumber, line, ex.getMessage()));
                }
                lineNumber++;
            }
        }

        return new MeasureParseResult(metadata, records, errors);
    }

    private BufferedReader toBufferedReader(Reader reader) {
        if (reader instanceof BufferedReader bufferedReader) {
            return bufferedReader;
        }
        return new BufferedReader(reader);
    }

    private MeasureRecord parseLine(MeasureFilenameMetadata metadata, String line, int lineNumber) {
        String[] elements = line.split(";", -1);
        return switch (metadata.kind()) {
            case MEDIDA_H_P1 -> parseP1(metadata, elements, line, lineNumber);
            case MEDIDA_QH_P2 -> parseP2(metadata, elements, line, lineNumber);
            case MEDIDA_CCH_F5 -> parseF5(metadata, elements, line, lineNumber);
            default -> throw new IllegalArgumentException(
                    Messages.format(LogsCommonMessageKey.MEASURE_FILE_KIND_UNSUPPORTED, metadata.kind()));
        };
    }

    private MeasureRecord parseP1(MeasureFilenameMetadata metadata, String[] elements, String line, int lineNumber) {
        validateExactColumnCount(metadata.originalFilename(), elements, lineNumber);
        ElementsCursor cursor = new ElementsCursor(elements);
        try {
            String cups = cursor.nextRaw();
            int tipoMedida = cursor.nextInt(metadata.originalFilename(), lineNumber);
            LocalDateTime timestamp = cursor.nextTimestamp(metadata.originalFilename(), lineNumber);
            float banderaInvVer = cursor.nextFloat(metadata.originalFilename(), lineNumber);
            float actent = cursor.nextFloat(metadata.originalFilename(), lineNumber);
            float qactent = cursor.nextFloat(metadata.originalFilename(), lineNumber);
            float actsal = cursor.nextFloat(metadata.originalFilename(), lineNumber);
            float qactsal = cursor.nextFloat(metadata.originalFilename(), lineNumber);
            float rQ1 = cursor.nextFloat(metadata.originalFilename(), lineNumber);
            float qrQ1 = cursor.nextFloat(metadata.originalFilename(), lineNumber);
            float rQ2 = cursor.nextFloat(metadata.originalFilename(), lineNumber);
            float qrQ2 = cursor.nextFloat(metadata.originalFilename(), lineNumber);
            float rQ3 = cursor.nextFloat(metadata.originalFilename(), lineNumber);
            float qrQ3 = cursor.nextFloat(metadata.originalFilename(), lineNumber);
            float rQ4 = cursor.nextFloat(metadata.originalFilename(), lineNumber);
            float qrQ4 = cursor.nextFloat(metadata.originalFilename(), lineNumber);
            float medres1 = cursor.nextFloat(metadata.originalFilename(), lineNumber);
            float qmedres1 = cursor.nextFloat(metadata.originalFilename(), lineNumber);
            float medres2 = cursor.nextFloat(metadata.originalFilename(), lineNumber);
            float qmedres2 = cursor.nextFloat(metadata.originalFilename(), lineNumber);
            int metodObt = cursor.nextInt(metadata.originalFilename(), lineNumber);
            int temporal = cursor.nextInt(metadata.originalFilename(), lineNumber);

            return new MeasureRecord.Hourly(
                    cups,
                    timestamp,
                    tipoMedida,
                    banderaInvVer,
                    actent,
                    qactent,
                    actsal,
                    qactsal,
                    rQ1,
                    qrQ1,
                    rQ2,
                    qrQ2,
                    rQ3,
                    qrQ3,
                    rQ4,
                    qrQ4,
                    medres1,
                    qmedres1,
                    medres2,
                    qmedres2,
                    metodObt,
                    temporal,
                    metadata.originalFilename(),
                    line
            );
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw invalidColumnCount(metadata.originalFilename(), elements, lineNumber, EXPECTED_P1_COLUMNS);
        }
    }

    private MeasureRecord parseP2(MeasureFilenameMetadata metadata, String[] elements, String line, int lineNumber) {
        if (elements.length < MIN_P2_COLUMNS || elements.length > MAX_P2_COLUMNS) {
            throw invalidColumnCount(metadata.originalFilename(), elements, lineNumber, MIN_P2_COLUMNS);
        }

        ElementsCursor cursor = new ElementsCursor(elements);
        try {
            String cups = cursor.nextRaw();
            int tipoMedida = cursor.nextInt(metadata.originalFilename(), lineNumber);
            LocalDateTime timestamp = cursor.nextTimestamp(metadata.originalFilename(), lineNumber);
            int banderaInvVer = cursor.nextInt(metadata.originalFilename(), lineNumber);
            int actent = cursor.nextInt(metadata.originalFilename(), lineNumber);
            int qactent = cursor.nextInt(metadata.originalFilename(), lineNumber);
            int actsal = cursor.nextInt(metadata.originalFilename(), lineNumber);
            int qactsal = cursor.nextInt(metadata.originalFilename(), lineNumber);
            int rQ1 = cursor.nextInt(metadata.originalFilename(), lineNumber);
            int qrQ1 = cursor.nextInt(metadata.originalFilename(), lineNumber);
            int rQ2 = cursor.nextInt(metadata.originalFilename(), lineNumber);
            int qrQ2 = cursor.nextInt(metadata.originalFilename(), lineNumber);
            int rQ3 = cursor.nextInt(metadata.originalFilename(), lineNumber);
            int qrQ3 = cursor.nextInt(metadata.originalFilename(), lineNumber);
            int rQ4 = cursor.nextInt(metadata.originalFilename(), lineNumber);
            int qrQ4 = cursor.nextInt(metadata.originalFilename(), lineNumber);
            int medres1 = cursor.nextInt(metadata.originalFilename(), lineNumber);
            int qmedres1 = cursor.nextInt(metadata.originalFilename(), lineNumber);
            int medres2 = cursor.nextInt(metadata.originalFilename(), lineNumber);
            int qmedres2 = cursor.nextInt(metadata.originalFilename(), lineNumber);
            int metodObt = cursor.nextInt(metadata.originalFilename(), lineNumber);
            Integer temporal = null;
            if (cursor.hasNext() && !cursor.peek().isBlank()) {
                temporal = cursor.nextInt(metadata.originalFilename(), lineNumber);
            }

            return new MeasureRecord.QuarterHourly(
                    cups,
                    timestamp,
                    tipoMedida,
                    banderaInvVer,
                    actent,
                    qactent,
                    actsal,
                    qactsal,
                    rQ1,
                    qrQ1,
                    rQ2,
                    qrQ2,
                    rQ3,
                    qrQ3,
                    rQ4,
                    qrQ4,
                    medres1,
                    qmedres1,
                    medres2,
                    qmedres2,
                    metodObt,
                    temporal,
                    metadata.originalFilename(),
                    line
            );
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw invalidColumnCount(metadata.originalFilename(), elements, lineNumber, MIN_P2_COLUMNS);
        }
    }

    private MeasureRecord parseF5(MeasureFilenameMetadata metadata, String[] elements, String line, int lineNumber) {
        validateF5Columns(metadata.originalFilename(), elements, lineNumber);

        ElementsCursor cursor = new ElementsCursor(elements);
        String cups = cursor.nextRaw();
        LocalDateTime timestamp = cursor.nextTimestamp(metadata.originalFilename(), lineNumber);
        int banderaInvVer = cursor.nextInt(metadata.originalFilename(), lineNumber);
        int actent = cursor.nextInt(metadata.originalFilename(), lineNumber);
        cursor.skipF5IntermediateFields();
        int metod = cursor.nextInt(metadata.originalFilename(), lineNumber);

        return new MeasureRecord.Cch(
                cups,
                timestamp,
                banderaInvVer,
                actent,
                metod,
                line
        );
    }

    private void validateF5Columns(String fileName, String[] elements, int lineNumber) {
        if (elements.length < EXPECTED_F5_COLUMNS) {
            throw invalidColumnCount(fileName, elements, lineNumber, EXPECTED_F5_COLUMNS);
        }

        for (int i = EXPECTED_F5_COLUMNS; i < elements.length; i++) {
            if (elements[i] != null && !elements[i].isBlank()) {
                throw invalidColumnCount(fileName, elements, lineNumber, EXPECTED_F5_COLUMNS);
            }
        }
    }

    private void validateExactColumnCount(String fileName, String[] elements, int lineNumber) {
        if (elements.length != EXPECTED_P1_COLUMNS) {
            throw invalidColumnCount(fileName, elements, lineNumber, EXPECTED_P1_COLUMNS);
        }
    }

    private IllegalArgumentException invalidColumnCount(
            String fileName,
            String[] elements,
            int lineNumber,
            int expectedCount
    ) {
        return new IllegalArgumentException(
                Messages.format(LogsCommonMessageKey.MEASURE_LINE_INVALID_COLUMN_COUNT,
                        Arrays.toString(elements), lineNumber, fileName, expectedCount));
    }

    private static final class ElementsCursor {
        private final String[] elements;
        private int index;

        private ElementsCursor(String[] elements) {
            this.elements = elements;
            this.index = 0;
        }

        private String nextRaw() {
            return elements[index++];
        }

        private int nextInt(String fileName, int lineNumber) {
            String value = nextRaw();
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ex) {
                throw conversionError(value, fileName, lineNumber);
            }
        }

        private float nextFloat(String fileName, int lineNumber) {
            String value = nextRaw();
            try {
                return Float.parseFloat(value);
            } catch (NumberFormatException ex) {
                throw conversionError(value, fileName, lineNumber);
            }
        }

        private LocalDateTime nextTimestamp(String fileName, int lineNumber) {
            String value = nextRaw();
            try {
                return LocalDateTime.parse(value, DATE_TIME_WITH_SECONDS_FORMATTER);
            } catch (DateTimeParseException ex) {
                try {
                    return LocalDateTime.parse(value, DATE_FORMATTER);
                } catch (DateTimeParseException ignored) {
                    throw conversionError(value, fileName, lineNumber);
                }
            }
        }

        private boolean hasNext() {
            return index < elements.length;
        }

        private String peek() {
            return elements[index];
        }

        /** Salta las columnas as1, rq1, rq2, rq3, rq4 del formato F5 que no se mapean a CCH. */
        private void skipF5IntermediateFields() {
            index += 5;
        }

        private IllegalArgumentException conversionError(String value, String fileName, int lineNumber) {
            return new IllegalArgumentException(
                    Messages.format(LogsCommonMessageKey.MEASURE_LINE_CONVERSION_ERROR,
                            value, lineNumber, fileName));
        }
    }
}
