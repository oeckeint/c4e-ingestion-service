package com.com4energy.processor.outbox.domain;

public enum OutboxEventType {

    FILE_REJECTED,
    FILE_ALREADY_EXISTS,
    FILE_UPLOADED,
    FILE_FAILED,

    /** Errores de parseo (formato de línea inválido). Genera .sge_defect.jsonl */
    FILE_PARSE_INCIDENT,

    /** Errores de validación semántica (CUPS, rangos, campos obligatorios). Genera .sge_defect.jsonl */
    FILE_VALIDATION_INCIDENT,

    /** Se creó un archivo agregado de defectos (.sge_defect.*). No publica cada línea individual. */
    FILE_DEFECT_REPORT_CREATED,

    /**
     * Binary split aisló registros que no pudieron persistirse en BD.
     * Genera .sge_quarantine.jsonl. Señal para retry manual o automático.
     */
    FILE_PERSISTENCE_QUARANTINE

}
