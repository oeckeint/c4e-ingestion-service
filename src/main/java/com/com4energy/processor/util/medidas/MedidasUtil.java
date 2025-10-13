package com.com4energy.processor.util.medidas;

import com.com4energy.processor.model.FileRecord;
import com.com4energy.processor.model.FileType;
import org.jetbrains.annotations.NotNull;

public class MedidasUtil {

    private MedidasUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static void defineMedidaType(@NotNull FileRecord fileRecord) {
        String firstSegment = fileRecord.getFilename().trim().split("_")[0];
        String startName = firstSegment.substring(0, Math.min(2, firstSegment.length())).toLowerCase();

        switch (startName) {
            case "f5":
                fileRecord.setType(FileType.MEDIDA_QH_F5);
                break;
            case "p5":
                fileRecord.setType(FileType.MEDIDA_QH_P5);
                break;
            case "p1":
                fileRecord.setType(FileType.MEDIDA_QH_P1);
                break;
            case "p2":
                fileRecord.setType(FileType.MEDIDA_QH_P2);
                break;
            default:
                fileRecord.setType(FileType.UNKNOWN);
        }

    }

}
