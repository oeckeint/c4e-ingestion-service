package com.com4energy.processor.model;

public enum FileType {
    MEDIDA_QH_F5,
    MEDIDA_QH_P5,
    MEDIDA_QH_P1,
    MEDIDA_QH_P2,
    H_MEASURE,
    FACTURA,
    OTRA_FACTURA,
    DOCUMENTO_PDF,
    UNKNOWN;

    @Override
    public String toString() {
        return name().replace("_", " ");
    }

}
