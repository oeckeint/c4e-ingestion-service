package com.com4energy.processor.util;

import com.com4energy.processor.model.FileType;

public class FileUtils {

    public static String extractExtension(String filename) {
        int lastDot = filename.lastIndexOf(".");
        return (lastDot > 0 && lastDot < filename.length() - 1)
                ? filename.substring(lastDot + 1)
                : "unknown";
    }

    public static FileType resolveFileType(String extension) {
        return switch (extension.toLowerCase()) {
            case "0" -> FileType.QH_MEASURE;
            case "xml" -> FileType.FACTURA;
            case "pdf" -> FileType.DOCUMENTO_PDF;
            default -> FileType.UNKNOWN;
        };
    }

}
