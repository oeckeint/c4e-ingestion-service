package com.com4energy.processor.common;

/**
 * Centralized field name constants for measure records (P1, P2, CCH).
 * Designed for extensibility to support future formats (F5 full support, etc.).
 * Naming convention:
 * - No prefix: common fields (P1 & P2)
 * - P2_: P2-specific fields
 * - CCH_: CCH-specific fields
 */
public class MeasureFieldNames {

    // === COMMON FIELDS (P1 & P2) ===
    public static final String ACTENT = "actent";
    public static final String QACTENT = "qactent";
    public static final String ACTSAL = "actsal";
    public static final String QACTSAL = "qactsal";
    public static final String RQ1 = "rQ1";
    public static final String QRQ1 = "qrQ1";
    public static final String RQ2 = "rQ2";
    public static final String QRQ2 = "qrQ2";
    public static final String RQ3 = "rQ3";
    public static final String QRQ3 = "qrQ3";
    public static final String RQ4 = "rQ4";
    public static final String QRQ4 = "qrQ4";
    public static final String MEDRES1 = "medres1";
    public static final String QMEDRES1 = "qmedres1";
    public static final String MEDRES2 = "medres2";
    public static final String QMEDRES2 = "qmedres2";

    // === P2 (QuarterHourly) SPECIFIC FIELDS ===
    public static final String P2_BANDERA_INV_VER = "banderaInvVer";

    // === CCH (medida_cch) FIELDS ===
    public static final String CCH_BANDERA_INV_VER = "banderaInvVer";
    public static final String CCH_ACTENT = "actent";
    public static final String CCH_METOD = "metod";


    private MeasureFieldNames() {
        // Utility class
    }
}

