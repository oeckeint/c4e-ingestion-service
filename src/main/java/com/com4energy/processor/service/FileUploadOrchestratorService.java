package com.com4energy.processor.service;

import com.com4energy.processor.common.IngestionCommonMessageKey;
import com.com4energy.processor.common.InternalServices;
import com.com4energy.processor.config.properties.FileUploadProperties;
import com.com4energy.processor.model.FailureReason;
import com.com4energy.processor.outbox.service.OutboxService;
import com.com4energy.processor.service.dto.FileContext;
import com.com4energy.processor.service.dto.FileHandlingResult;
import com.com4energy.processor.service.dto.FileBatchResult;
import com.com4energy.processor.service.factory.FileContextMessageFactory;
import com.com4energy.processor.service.validation.FileValidator;
import com.com4energy.processor.service.validation.ValidationContext;
import com.com4energy.processor.util.FileStorageUtil;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
@Slf4j
public class FileUploadOrchestratorService {

    private final FileUploadProperties fileUploadProperties;
    private final FileRecordService fileRecordService;
    private final OutboxService outboxService;
    private final FileStorageUtil fileStorageUtil;
    private final List<FileValidator> validators;
    private final FileContextMessageFactory fileContextMessageFactory;

    public FileBatchResult processFiles(MultipartFile[] files) {
        FileBatchResult fileBatchResult = FileBatchResult.fromFileContexts(getFileContexts(files));

        fileBatchResult.failedFiles().forEach(this::processRejectFile);
        fileBatchResult.duplicatedFiles().forEach(this::processDuplicatedFile);
        fileBatchResult.validFiles().forEach(this::processNewFile);

        return fileBatchResult;
    }

    public List<FileContext> getFileContexts(MultipartFile[] files) {
        return files == null ? List.of() :
                Arrays.stream(files)
                .map(this::runValidationsForSingleFile)
                .toList();
    }

    private FileContext runValidationsForSingleFile(MultipartFile file) {
        ValidationContext validationContext = ValidationContext.from(file);
        List<FailureReason> errors = new ArrayList<>();

        for (FileValidator fileValidator : this.validators) {
            Optional<FailureReason> result = fileValidator.validate(validationContext);

            if (result.isPresent()) {
                FailureReason reason = result.get();
                errors.add(reason);

                if (fileValidator.isFailFast())
                    return switch (reason) {
                        case DUPLICATED_ORIGINAL_FILENAME -> FileContext.fromWithDuplicatedOriginalFilenameStatus(validationContext, errors, reason);
                        case DUPLICATED_CONTENT -> FileContext.fromWithDuplicatedContentStatus(validationContext, errors, reason);
                        default -> FileContext.fromWithCriticalValidationFailedStatus(validationContext, errors, reason);
                    };
            }
        }

        if (!errors.isEmpty())
            return FileContext.fromWithValidationFailed(validationContext, errors);

        return FileContext.fromWithValidStatus(validationContext);
    }

    public FileHandlingResult processRejectFile(@NonNull FileContext fileContext) {
        if (!fileContext.isInvalid())
            throw new IllegalArgumentException(fileContextMessageFactory.format(IngestionCommonMessageKey.FILE_REJECT_EXPECTS_INVALID_CONTEXT_ERROR, fileContext));

        FileHandlingResult afterStorageResult = fileStorageUtil.saveInDiskOverridingExisting(Paths.get(fileUploadProperties.rejectedPath()), fileContext);
        return this.outboxService.saveRejected(afterStorageResult, InternalServices.CHAIN_VALIDATION);
    }

    public FileHandlingResult processNewFile(@NonNull FileContext fileContext) {
        if (fileContext.isInvalid())
            throw new IllegalArgumentException(fileContextMessageFactory.format(IngestionCommonMessageKey.FILE_SAVE_NEW_EXPECTS_VALID_CONTEXT_ERROR, fileContext));

        FileHandlingResult afterStorageResult = fileStorageUtil.saveInDiskOverridingExisting(Paths.get(fileUploadProperties.automaticPath()), fileContext);
        return fileRecordService.saveNew(afterStorageResult);
    }

    public FileHandlingResult processDuplicatedFile(@NonNull FileContext fileContext) {
        if (fileContext.isDuplicated()) {
            FileHandlingResult afterStorageResult = fileStorageUtil.saveInDiskOverridingExisting(Paths.get(fileUploadProperties.duplicatesPath()), fileContext);
            return outboxService.saveDuplicated(afterStorageResult, InternalServices.CHAIN_VALIDATION);
        }

        throw new IllegalArgumentException(fileContextMessageFactory.format(IngestionCommonMessageKey.FILE_SAVE_DUPLICATED_EXPECTS_DUPLICATED_CONTEXT_ERROR, fileContext));
    }

}
