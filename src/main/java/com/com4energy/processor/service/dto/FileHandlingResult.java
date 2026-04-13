package com.com4energy.processor.service.dto;

/**
 * Operational outcome of rejection handling side effects.
 */
public record FileHandlingResult(
        FileContext fileContext,
        boolean storedInDisk,
        boolean persistedInFileRecords,
        boolean persistedInOutboxEvent
) {

    public static FileHandlingResult initial(FileContext fileContext) {
        return new FileHandlingResult(fileContext, false, false, false);
    }

    public FileHandlingResult withFileContext(FileContext fileContext) {
        return new FileHandlingResult(fileContext, storedInDisk, persistedInFileRecords, persistedInOutboxEvent);
    }

    public FileHandlingResult withStoredInDisk() {
        return new FileHandlingResult(fileContext, true, persistedInFileRecords, persistedInOutboxEvent);
    }

    public FileHandlingResult withPersistedInFileRecords() {
        return new FileHandlingResult(fileContext, storedInDisk, true, persistedInOutboxEvent);
    }

    public FileHandlingResult withPersistedInOutboxEvent() {
        return new FileHandlingResult(fileContext, storedInDisk, persistedInFileRecords, true);
    }

}
