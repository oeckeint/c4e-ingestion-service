package com.com4energy.processor.model;

import com.com4energy.i18n.core.Messages;
import com.com4energy.processor.common.LogsCommonMessageKey;
import com.com4energy.processor.service.measure.InvalidMeasureFilenameException;

public enum FileType {
    MEDIDA_CCH_F5,
    MEDIDA_H_P1,
    MEDIDA_QH_P2,
    AWAITING_CLASSIFICATION,
    H_MEASURE,
    FACTURA,
    OTRA_FACTURA,
    DOCUMENTO_PDF,
    UNKNOWN;

    @Override
    public String toString() {
        return name().replace("_", " ");
    }

    public static FileType fromMeasurePrefix(String token) {
        if (token == null || token.length() < 2) {
            throw new InvalidMeasureFilenameException("El nombre del archivo no contiene un prefijo de medida válido");
        }

        return switch (token.substring(0, 2).toLowerCase()) {
            case "f5" -> MEDIDA_CCH_F5;
            case "p1" -> MEDIDA_H_P1;
            case "p2" -> MEDIDA_QH_P2;
            default -> throw new InvalidMeasureFilenameException(
                    Messages.format(LogsCommonMessageKey.MEASURE_FILENAME_UNKNOWN_TYPE));
        };
    }
}
