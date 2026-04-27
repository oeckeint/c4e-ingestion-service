package com.com4energy.processor.service.measure;

import com.com4energy.processor.model.FileType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MeasureFileParserServiceTest {

    private static final String VALID_P1_FILENAME = "P1AA_BBBB_CCCC_DDDDDDD.0";
    private static final String VALID_P2_FILENAME = "P2AA_BBBB_CCCC_DDDDDDD.1";
    private static final String VALID_F5_FILENAME = "F5AA_BBBB_CCCC_DDDDDDD.2";

    private final MeasureFileParserService service = new MeasureFileParserService();

    @Test
    void parseP1BuildsHourlyMeasureRecords() throws IOException {
        MeasureParseResult result = service.parse(VALID_P1_FILENAME, new StringReader(
                "ES123;1;2025/01/01 00:00;1.0;2.0;3.0;4.0;5.0;6.0;7.0;8.0;9.0;10.0;11.0;12.0;13.0;14.0;15.0;16.0;17.0;18;19"
        ), FileType.MEDIDA_H_P1);

        assertEquals(FileType.MEDIDA_H_P1, result.metadata().kind());
        assertEquals(FileType.MEDIDA_H_P1, result.metadata().fileType());
        assertEquals(1, result.successCount());
        assertEquals(0, result.errorCount());

        MeasureRecord.Hourly hourly = assertInstanceOf(MeasureRecord.Hourly.class, result.records().get(0));
        assertEquals("ES123", hourly.cups());
        assertEquals(1, hourly.tipoMedida());
        assertEquals(19, hourly.temporal());
        assertEquals(17.0f, hourly.qmedres2());
        assertEquals(VALID_P1_FILENAME, hourly.origen());
    }

    @Test
    void parseP2LeavesTemporalNullWhenColumnIsMissing() throws IOException {
        MeasureParseResult result = service.parse(VALID_P2_FILENAME, new StringReader(
                "ES456;2;2025/01/01 00:15;1;2;3;4;5;6;7;8;9;10;11;12;13;14;15;16;17;18"
        ), FileType.MEDIDA_QH_P2);

        assertEquals(1, result.successCount());
        assertEquals(0, result.errorCount());

        MeasureRecord.QuarterHourly quarterHourly =
                assertInstanceOf(MeasureRecord.QuarterHourly.class, result.records().get(0));
        assertNull(quarterHourly.temporal());
        assertEquals(VALID_P2_FILENAME, quarterHourly.origen());
    }

    @Test
    void parseP2AcceptsUpToTwentyThreeColumns() throws IOException {
        MeasureParseResult result = service.parse(VALID_P2_FILENAME, new StringReader(
                "ES456;2;2025/01/01 00:15;1;2;3;4;5;6;7;8;9;10;11;12;13;14;15;16;17;18;19;EXTRA"
        ), FileType.MEDIDA_QH_P2);

        assertEquals(1, result.successCount());
        assertEquals(0, result.errorCount());

        MeasureRecord.QuarterHourly quarterHourly =
                assertInstanceOf(MeasureRecord.QuarterHourly.class, result.records().get(0));
        assertEquals(19, quarterHourly.temporal());
    }

    @Test
    void parseF5BuildsCchMeasureRecords() throws IOException {
        MeasureParseResult result = service.parse(VALID_F5_FILENAME, new StringReader(
                "ES789;2025/01/01 01:00;1;2;3;4;5;6;7;8;9;FAC-001"
        ), FileType.MEDIDA_CCH_F5);

        assertEquals(1, result.successCount());
        MeasureRecord.Cch cch = assertInstanceOf(MeasureRecord.Cch.class, result.records().get(0));
        assertEquals(2, cch.actent());
        assertEquals(8, cch.metod());
        assertEquals(FileType.MEDIDA_CCH_F5, cch.kind());
    }

    @Test
    void parseF5AllowsTrailingEmptyColumns() throws IOException {
        MeasureParseResult result = service.parse(VALID_F5_FILENAME, new StringReader(
                "ES789;2025/01/01 01:00;1;2;3;4;5;6;7;8;9;FAC-001;;;"
        ), FileType.MEDIDA_CCH_F5);

        assertEquals(1, result.successCount());
        assertEquals(0, result.errorCount());
        MeasureRecord.Cch cch = assertInstanceOf(MeasureRecord.Cch.class, result.records().get(0));
        assertEquals(2, cch.actent());
        assertEquals(8, cch.metod());
    }

    @Test
    void parseF5RejectsNonEmptyExtraColumns() throws IOException {
        MeasureParseResult result = service.parse(VALID_F5_FILENAME, new StringReader(
                "ES789;2025/01/01 01:00;1;2;3;4;5;6;7;8;9;FAC-001;EXTRA"
        ), FileType.MEDIDA_CCH_F5);

        assertEquals(0, result.successCount());
        assertEquals(1, result.errorCount());
        // Error message should exist (may be key reference or formatted message)
        assertTrue(result.errors().size() > 0);
        assertTrue(result.errors().get(0).message() != null && !result.errors().get(0).message().isEmpty());
    }

    @Test
    void parseCollectsLineErrorsAndContinues() throws IOException {
        String content = String.join("\n",
                "ES123;1;2025/01/01 00:00;1.0;2.0;3.0;4.0;5.0;6.0;7.0;8.0;9.0;10.0;11.0;12.0;13.0;14.0;15.0;16.0;17.0;18;19",
                "ES123;1;BAD_DATE;1.0;2.0;3.0;4.0;5.0;6.0;7.0;8.0;9.0;10.0;11.0;12.0;13.0;14.0;15.0;16.0;17.0;18;19"
        );

        MeasureParseResult result = service.parse(VALID_P1_FILENAME, new StringReader(content), FileType.MEDIDA_H_P1);

        assertEquals(1, result.successCount());
        assertEquals(1, result.errorCount());
        assertTrue(result.hasErrors());
        assertEquals(2, result.errors().get(0).lineNumber());
        // Error message should exist
        assertTrue(result.errors().get(0).message() != null);
    }

    @Test
    void parseP1AcceptsTimestampWithSeconds() throws IOException {
        MeasureParseResult result = service.parse(VALID_P1_FILENAME, new StringReader(
                "ES123;1;2025/01/01 00:00:00;1.0;2.0;3.0;4.0;5.0;6.0;7.0;8.0;9.0;10.0;11.0;12.0;13.0;14.0;15.0;16.0;17.0;18;19"
        ), FileType.MEDIDA_H_P1);

        assertEquals(1, result.successCount());
        assertEquals(0, result.errorCount());
        MeasureRecord.Hourly hourly = assertInstanceOf(MeasureRecord.Hourly.class, result.records().get(0));
        assertEquals(2025, hourly.timestamp().getYear());
        assertEquals(0, hourly.timestamp().getSecond());
    }

}

