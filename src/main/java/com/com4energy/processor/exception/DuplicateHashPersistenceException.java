package com.com4energy.processor.exception;

import lombok.Getter;

/**
 * Domain-level signal that a file cannot be persisted because its content hash already exists.
 */
@Getter
public class DuplicateHashPersistenceException extends RuntimeException {

    private final String hash;

    public DuplicateHashPersistenceException(String hash, Throwable cause) {
        super("Duplicate content hash detected while persisting file record", cause);
        this.hash = hash;
    }

}
