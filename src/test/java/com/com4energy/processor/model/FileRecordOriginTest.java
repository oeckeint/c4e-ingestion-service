package com.com4energy.processor.model;

import com.com4energy.processor.service.dto.FileContext;
import com.com4energy.processor.service.validation.ValidationContext;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FileRecordOriginTest {

    @Test
    void fromDefaultsToApiOrigin() {
        FileRecord record = FileRecord.from(validContext("api-file.xml"));

        assertEquals(FileOrigin.API, record.getOrigin());
        assertEquals(FileType.AWAITING_CLASSIFICATION, record.getType());
    }

    @Test
    void fromUsesExplicitOriginWhenProvided() {
        FileRecord record = FileRecord.from(validContext("job-file.xml"), FileOrigin.JOB);

        assertEquals(FileOrigin.JOB, record.getOrigin());
        assertEquals(FileType.AWAITING_CLASSIFICATION, record.getType());
    }

    @Test
    void fromDetectsMeasureTypeByFilenamePrefix() {
        FileRecord record = FileRecord.from(validContext("P1AA_BBBB_CCCC_DDDDDDD.0"));

        assertEquals(FileType.MEDIDA_H_P1, record.getType());
    }

    private FileContext validContext(String filename) {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                filename,
                "application/xml",
                "<MensajeFacturacion/>".getBytes()
        );
        return FileContext.fromWithValidStatus(ValidationContext.from(file));
    }
}

