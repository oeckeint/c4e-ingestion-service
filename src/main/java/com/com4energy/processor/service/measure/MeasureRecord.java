package com.com4energy.processor.service.measure;

import com.com4energy.processor.model.FileType;

import java.time.LocalDateTime;

public sealed interface MeasureRecord permits MeasureRecord.Hourly,
        MeasureRecord.QuarterHourly, MeasureRecord.Cch {

    FileType kind();

    String cups();

    LocalDateTime timestamp();

    String rawLine();

    record Hourly(
            String cups,
            LocalDateTime timestamp,
            int tipoMedida,
            float banderaInvVer,
            float actent,
            float qactent,
            float actsal,
            float qactsal,
            float rQ1,
            float qrQ1,
            float rQ2,
            float qrQ2,
            float rQ3,
            float qrQ3,
            float rQ4,
            float qrQ4,
            float medres1,
            float qmedres1,
            float medres2,
            float qmedres2,
            int metodObt,
            int temporal,
            String origen,
            String rawLine
    ) implements MeasureRecord {
        @Override
        public FileType kind() {
            return FileType.MEDIDA_H_P1;
        }
    }

    record QuarterHourly(
            String cups,
            LocalDateTime timestamp,
            int tipoMedida,
            int banderaInvVer,
            int actent,
            int qactent,
            int actsal,
            int qactsal,
            int rQ1,
            int qrQ1,
            int rQ2,
            int qrQ2,
            int rQ3,
            int qrQ3,
            int rQ4,
            int qrQ4,
            int medres1,
            int qmedres1,
            int medres2,
            int qmedres2,
            int metodObt,
            Integer temporal,
            String origen,
            String rawLine
    ) implements MeasureRecord {
        @Override
        public FileType kind() {
            return FileType.MEDIDA_QH_P2;
        }
    }


    record Cch(
            FileType kind,
            String cups,
            LocalDateTime timestamp,
            int banderaInvVer,
            int actent,
            int metod,
            String rawLine
    ) implements MeasureRecord {
        public Cch(
                String cups,
                LocalDateTime timestamp,
                int banderaInvVer,
                int actent,
                int metod,
                String rawLine
        ) {
            this(FileType.MEDIDA_CCH_F5, cups, timestamp, banderaInvVer, actent, metod, rawLine);
        }
    }
}

