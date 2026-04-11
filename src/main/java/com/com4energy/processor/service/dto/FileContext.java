package com.com4energy.processor.service.dto;

import com.com4energy.i18n.core.Messages;
import com.com4energy.processor.api.response.FileMetadata;
import com.com4energy.processor.common.IngestionCommonMessageKey;
import com.com4energy.processor.model.FailureReason;
import com.com4energy.processor.model.FileStatus;
import com.com4energy.processor.service.validation.ValidationContext;
import lombok.NonNull;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public record FileContext(
        ValidationContext validationContext,
        List<FailureReason> failureReasons,
        FailureReason failFastReason,
        Path storedFilePath,
        FileStatus fileStatus) {

    public FileContext {
        if (validationContext == null) {
            throw new IllegalArgumentException(Messages.get(IngestionCommonMessageKey.FAILED_FILE_CONTEXT_VALIDATION_CONTEXT_NULL));
        }

        failureReasons = List.copyOf(failureReasons);

        if (fileStatus == null) {
            throw new IllegalArgumentException(Messages.get(IngestionCommonMessageKey.FAILED_FILE_CONTEXT_FILE_STATUS_NULL));
        }
    }

    public static FileContext fromWithValidationFailed(@NonNull ValidationContext validationContext, List<FailureReason> allReasons) {
        return new FileContext(validationContext, allReasons, null, null, FileStatus.VALIDATION_FAILED);
    }

    public static FileContext fromWithCriticalValidationFailedStatus(@NonNull ValidationContext validationContext, List<FailureReason> allReasons, FailureReason failFastReason) {
        return new FileContext(validationContext, allReasons, failFastReason, null, FileStatus.CRITICAL_VALIDATION_FAILED);
    }

    public static FileContext fromWithDuplicatedOriginalFilenameStatus(@NonNull ValidationContext validationContext,
                                                                        List<FailureReason> allReasons,
                                                                        FailureReason failFastReason) {
        return new FileContext(validationContext, allReasons, failFastReason, null, FileStatus.DUPLICATED_ORIGINAL_FILENAME);
    }

    public static FileContext fromWithDuplicatedContentStatus(@NonNull ValidationContext validationContext,
                                                               List<FailureReason> allReasons,
                                                               FailureReason failFastReason) {
        return new FileContext(validationContext, allReasons, failFastReason, null, FileStatus.DUPLICATED_CONTENT);
    }

    public static FileContext fromWithValidStatus(@NonNull ValidationContext validationContext) {
        return new FileContext(validationContext, List.of(), null, null, FileStatus.VALID);
    }

    public FileContext withStoredPath(@NonNull Path storedFilePath) {
        return new FileContext(validationContext, failureReasons, failFastReason, storedFilePath, fileStatus);
    }

    public FileContext withStatus(@NonNull FileStatus fileStatus) {
        return new FileContext(validationContext, failureReasons, failFastReason, storedFilePath, fileStatus);
    }

    public Optional<String> findStoredFilePath() {
        return Optional.ofNullable(storedFilePath).map(Path::toString);
    }

    public Optional<FailureReason> findFailFastReason() {
        return Optional.ofNullable(failFastReason);
    }

    public FailureReason getFirstFailureReason() {
        return failureReasons.get(0);
    }

    public FailureReason getPrimaryFailureReason() {
        return findFailFastReason().orElseGet(this::getFirstFailureReason);
    }

    public boolean isOriginalFilenameDuplicated() {
        return failureReasons.contains(FailureReason.DUPLICATED_ORIGINAL_FILENAME)
                || failureReasons.contains(FailureReason.DUPLICATED_FILENAME)
                || fileStatus == FileStatus.DUPLICATED_ORIGINAL_FILENAME;
    }

    public boolean isContentDuplicated() {
        return failureReasons.contains(FailureReason.DUPLICATED_CONTENT)
                || fileStatus == FileStatus.DUPLICATED_CONTENT;
    }

    public boolean isDuplicated(){
        return isOriginalFilenameDuplicated() || isContentDuplicated();
    }

    public boolean isInvalid() {
        return fileStatus != FileStatus.VALID;
    }

    public FileMetadata toFileMetadata() {
        return FileMetadata.builder()
                .originalFilename(validationContext().getOriginalFilename())
                .status(fileStatus)
                .hash(validationContext().getOrComputeHash())
                .build();
    }

}
