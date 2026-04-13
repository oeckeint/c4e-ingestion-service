package com.com4energy.processor.service.measure;

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

    private static final int EXPECTED_FILENAME_LENGTH = 24;
    private static final int EXPECTED_NAME_TOKENS = 4;
    private static final int EXPECTED_P1_COLUMNS = 22;
    private static final int MIN_P2_COLUMNS = 21;
    private static final int MAX_P2_COLUMNS = 22;
    private static final int EXPECTED_F5_COLUMNS = 12;
    private static final int EXPECTED_P5_COLUMNS = 5;
    private static final int DEFAULT_P2_TEMPORAL = 99;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
    private static final DateTimeFormatter DATE_TIME_WITH_SECONDS_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    public MeasureParseResult parse(Path file) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return parse(file.getFileName().toString(), reader);
        }
    }

    public MeasureParseResult parse(String fileName, Reader reader) throws IOException {
        MeasureFilenameMetadata metadata = parseFilename(fileName);
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

    public MeasureFilenameMetadata parseFilename(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new InvalidMeasureFilenameException("El nombre del archivo de medidas es obligatorio");
        }
        if (fileName.contains(" ")) {
            throw new InvalidMeasureFilenameException("El nombre del archivo de medidas no puede contener espacios");
        }
        if (fileName.length() != EXPECTED_FILENAME_LENGTH) {
            throw new InvalidMeasureFilenameException(
                    "El nombre del archivo de medidas debe tener exactamente 24 caracteres"
            );
        }

        String[] tokens = fileName.split("_");
        if (tokens.length != EXPECTED_NAME_TOKENS) {
            throw new InvalidMeasureFilenameException(
                    "El nombre del archivo de medidas no contiene la cantidad esperada de segmentos"
            );
        }

        String[] tailParts = tokens[EXPECTED_NAME_TOKENS - 1].split("\\.");
        if (tailParts.length != 2) {
            throw new InvalidMeasureFilenameException("El archivo de medidas debe incluir una extensión numérica");
        }

        String extension = tailParts[1];
        if (extension.length() != 1 || !Character.isDigit(extension.charAt(0))) {
            throw new InvalidMeasureFilenameException("La extensión del archivo de medidas debe ser un dígito entre 0 y 9");
        }

        List<String> normalizedTokens = new ArrayList<>(Arrays.asList(tokens));
        normalizedTokens.set(EXPECTED_NAME_TOKENS - 1, tailParts[0]);

        return new MeasureFilenameMetadata(
                fileName,
                MeasureFileKind.fromPrefix(tokens[0]),
                normalizedTokens,
                Integer.parseInt(extension)
        );
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
            case P1 -> parseP1(metadata, elements, line, lineNumber);
            case P2 -> parseP2(metadata, elements, line, lineNumber);
            case F5 -> parseF5(metadata, elements, line, lineNumber);
            case P5 -> parseP5(metadata, elements, line, lineNumber);
        };
    }

    private MeasureRecord parseP1(MeasureFilenameMetadata metadata, String[] elements, String line, int lineNumber) {
        validateExactColumnCount(metadata.originalFilename(), elements, lineNumber, EXPECTED_P1_COLUMNS);
        int cursor = 0;
        try {
            String cups = elements[cursor++];
            int tipoMedida = parseInteger(elements[cursor], metadata.originalFilename(), lineNumber);
            cursor++;
            LocalDateTime timestamp = parseTimestamp(elements[cursor], metadata.originalFilename(), lineNumber);
            cursor++;
            float banderaInvVer = parseFloat(elements[cursor], metadata.originalFilename(), lineNumber);
            cursor++;
            float actent = parseFloat(elements[cursor], metadata.originalFilename(), lineNumber);
            cursor++;
            float qactent = parseFloat(elements[cursor], metadata.originalFilename(), lineNumber);
            cursor++;
            float actsal = parseFloat(elements[cursor], metadata.originalFilename(), lineNumber);
            cursor++;
            float qactsal = parseFloat(elements[cursor], metadata.originalFilename(), lineNumber);
            cursor++;
            float rQ1 = parseFloat(elements[cursor], metadata.originalFilename(), lineNumber);
            cursor++;
            float qrQ1 = parseFloat(elements[cursor], metadata.originalFilename(), lineNumber);
            cursor++;
            float rQ2 = parseFloat(elements[cursor], metadata.originalFilename(), lineNumber);
            cursor++;
            float qrQ2 = parseFloat(elements[cursor], metadata.originalFilename(), lineNumber);
            cursor++;
            float rQ3 = parseFloat(elements[cursor], metadata.originalFilename(), lineNumber);
            cursor++;
            float qrQ3 = parseFloat(elements[cursor], metadata.originalFilename(), lineNumber);
            cursor++;
            float rQ4 = parseFloat(elements[cursor], metadata.originalFilename(), lineNumber);
            cursor++;
            float qrQ4 = parseFloat(elements[cursor], metadata.originalFilename(), lineNumber);
            cursor++;
            float medres1 = parseFloat(elements[cursor], metadata.originalFilename(), lineNumber);
            cursor++;
            float qmedres1 = parseFloat(elements[cursor], metadata.originalFilename(), lineNumber);
            cursor++;
            float medres2 = parseFloat(elements[cursor], metadata.originalFilename(), lineNumber);
            cursor++;
            float qmedres2 = parseFloat(elements[cursor], metadata.originalFilename(), lineNumber);
            cursor++;
            int metodObt = parseInteger(elements[cursor], metadata.originalFilename(), lineNumber);
            cursor++;
            int temporal = parseInteger(elements[cursor], metadata.originalFilename(), lineNumber);

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

        int cursor = 0;
        try {
            String cups = elements[cursor++];
            int tipoMedida = parseInteger(elements[cursor], metadata.originalFilename(), lineNumber);
            cursor++;
            LocalDateTime timestamp = parseTimestamp(elements[cursor], metadata.originalFilename(), lineNumber);
            cursor++;
            int banderaInvVer = parseInteger(elements[cursor], metadata.originalFilename(), lineNumber);
            cursor++;
            int actent = parseInteger(elements[cursor], metadata.originalFilename(), lineNumber);
            cursor++;
            int qactent = parseInteger(elements[cursor], metadata.originalFilename(), lineNumber);
            cursor++;
            int actsal = parseInteger(elements[cursor], metadata.originalFilename(), lineNumber);
            cursor++;
            int qactsal = parseInteger(elements[cursor], metadata.originalFilename(), lineNumber);
            cursor++;
            int rQ1 = parseInteger(elements[cursor], metadata.originalFilename(), lineNumber);
            cursor++;
            int qrQ1 = parseInteger(elements[cursor], metadata.originalFilename(), lineNumber);
            cursor++;
            int rQ2 = parseInteger(elements[cursor], metadata.originalFilename(), lineNumber);
            cursor++;
            int qrQ2 = parseInteger(elements[cursor], metadata.originalFilename(), lineNumber);
            cursor++;
            int rQ3 = parseInteger(elements[cursor], metadata.originalFilename(), lineNumber);
            cursor++;
            int qrQ3 = parseInteger(elements[cursor], metadata.originalFilename(), lineNumber);
            cursor++;
            int rQ4 = parseInteger(elements[cursor], metadata.originalFilename(), lineNumber);
            cursor++;
            int qrQ4 = parseInteger(elements[cursor], metadata.originalFilename(), lineNumber);
            cursor++;
            int medres1 = parseInteger(elements[cursor], metadata.originalFilename(), lineNumber);
            cursor++;
            int qmedres1 = parseInteger(elements[cursor], metadata.originalFilename(), lineNumber);
            cursor++;
            int medres2 = parseInteger(elements[cursor], metadata.originalFilename(), lineNumber);
            cursor++;
            int qmedres2 = parseInteger(elements[cursor], metadata.originalFilename(), lineNumber);
            cursor++;
            int metodObt = parseInteger(elements[cursor], metadata.originalFilename(), lineNumber);
            cursor++;
            int temporal = elements.length > cursor
                    ? parseInteger(elements[cursor], metadata.originalFilename(), lineNumber)
                    : DEFAULT_P2_TEMPORAL;

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

        return new MeasureRecord.Legacy(
                elements[0],
                parseTimestamp(elements[1], metadata.originalFilename(), lineNumber),
                parseInteger(elements[2], metadata.originalFilename(), lineNumber),
                parseInteger(elements[3], metadata.originalFilename(), lineNumber),
                parseInteger(elements[4], metadata.originalFilename(), lineNumber),
                parseInteger(elements[5], metadata.originalFilename(), lineNumber),
                parseInteger(elements[6], metadata.originalFilename(), lineNumber),
                parseInteger(elements[7], metadata.originalFilename(), lineNumber),
                parseInteger(elements[8], metadata.originalFilename(), lineNumber),
                parseInteger(elements[9], metadata.originalFilename(), lineNumber),
                parseInteger(elements[10], metadata.originalFilename(), lineNumber),
                elements[11],
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

    private MeasureRecord parseP5(MeasureFilenameMetadata metadata, String[] elements, String line, int lineNumber) {
        validateExactColumnCount(metadata.originalFilename(), elements, lineNumber, EXPECTED_P5_COLUMNS);

        return new MeasureRecord.Cch(
                elements[0],
                parseTimestamp(elements[1], metadata.originalFilename(), lineNumber),
                parseInteger(elements[2], metadata.originalFilename(), lineNumber),
                parseInteger(elements[3], metadata.originalFilename(), lineNumber),
                parseInteger(elements[4], metadata.originalFilename(), lineNumber),
                line
        );
    }

    private void validateExactColumnCount(String fileName, String[] elements, int lineNumber, int expectedCount) {
        if (elements.length != expectedCount) {
            throw invalidColumnCount(fileName, elements, lineNumber, expectedCount);
        }
    }

    private IllegalArgumentException invalidColumnCount(
            String fileName,
            String[] elements,
            int lineNumber,
            int expectedCount
    ) {
        return new IllegalArgumentException(
                "La entrada " + Arrays.toString(elements)
                        + " en la línea " + lineNumber
                        + " del archivo " + fileName
                        + " no coincide con la cantidad de datos esperados ("
                        + expectedCount + ")."
        );
    }

    private LocalDateTime parseTimestamp(String value, String fileName, int lineNumber) {
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

    private int parseInteger(String value, String fileName, int lineNumber) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw conversionError(value, fileName, lineNumber);
        }
    }

    private float parseFloat(String value, String fileName, int lineNumber) {
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException ex) {
            throw conversionError(value, fileName, lineNumber);
        }
    }

    private IllegalArgumentException conversionError(String value, String fileName, int lineNumber) {
        return new IllegalArgumentException(
                "No se pudo convertir el elemento " + value
                        + " en la línea " + lineNumber
                        + " del archivo " + fileName
        );
    }
}

