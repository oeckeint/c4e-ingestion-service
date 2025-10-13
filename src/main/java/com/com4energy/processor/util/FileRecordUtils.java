package com.com4energy.processor.util;

import com.com4energy.processor.model.FileRecord;
import com.com4energy.processor.model.FileType;
import com.com4energy.processor.util.medidas.MedidasUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class FileRecordUtils {

    private FileRecordUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static void defineAndSetFileTypeToFileRecord(@NotNull FileRecord record, File file){
        record.setType(FileType.UNKNOWN);
        switch (record.getExtension()) {
            case "xml":
                defineXmlType(record, file);
                break;
            case "0":
            case "1":
            case "2":
            case "3":
            case "4":
            case "5":
            case "6":
            case "7":
            case "8":
            case "9":
                MedidasUtil.defineMedidaType(record);
                break;
        }
    }

    private static @NotNull FileRecord defineXmlType (@NotNull FileRecord record, File file){
        record.setType(FileType.FACTURA);
        return record;
    }

}
