package com.com4energy.processor.common;

import com.com4energy.i18n.core.MessageKey;

public enum IngestionExceptionMessageKey implements MessageKey {

    FILE_STORAGE_ERROR("ingestion.exception.file.storage.error"),
    FILE_DATABASE_ERROR("ingestion.exception.file.database.error"),
    FILE_HASH_ERROR("ingestion.exception.file.hash.error"),
    FILE_VALIDATOR_ERROR("ingestion.exception.file.validator.error"),
    FILE_ROLLBACK_ERROR("ingestion.exception.file.rollback.error"),
    FILE_DUPLICATE_ERROR("ingestion.exception.file.duplicate.error"),
    FILE_DATA_INTEGRITY_CORRUPTION_ERROR("ingestion.exception.file.data-integrity-corruption.error");

    private final String key;

    IngestionExceptionMessageKey(String key) {
        this.key = key;
    }

    @Override
    public String key() {
        return key;
    }

}

