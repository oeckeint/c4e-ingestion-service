package com.com4energy.processor.service;

import com.com4energy.processor.common.IngestionCommonMessageKey;
import com.com4energy.processor.common.InternalServices;
import com.com4energy.processor.config.properties.FileUploadProperties;
import com.com4energy.processor.exception.DuplicateHashPersistenceException;
import com.com4energy.processor.model.FailureReason;
import com.com4energy.processor.model.FileOrigin;
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
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
        return processFiles(files, FileOrigin.API);
    }

    public FileBatchResult processFiles(MultipartFile[] files, FileOrigin origin) {
        if (files == null || files.length == 0) {
            return FileBatchResult.empty();
        }

        // Sort alphabetically so that, when two files have the same content,
        // the one with the lexicographically smaller name (e.g. ".0" before ".1")
        // is always registered first, making the duplicate-winner deterministic.
        MultipartFile[] sortedFiles = Arrays.stream(files)
                .sorted(Comparator.comparing(f -> {
                    String name = f.getOriginalFilename();
                    return name != null ? name : "";
                }))
                .toArray(MultipartFile[]::new);

        List<FileContext> finalizedContexts = new ArrayList<>(sortedFiles.length);
        Set<String> batchHashes = HashSet.newHashSet(Math.max(16, sortedFiles.length * 2));

        for (MultipartFile file : sortedFiles) {
            finalizedContexts.add(processSingleFile(file, origin, batchHashes));
        }

        return FileBatchResult.fromFileContexts(finalizedContexts);
    }

    private FileContext processSingleFile(MultipartFile file, FileOrigin origin, Set<String> batchHashes) {
        FileContext fileContext = runValidationsForSingleFile(file);
        if (fileContext.isInvalid()) {
            handleInvalidFileContext(fileContext, origin);
            return fileContext;
        }

        return handleValidFileContext(fileContext, origin, batchHashes);
    }

    private void handleInvalidFileContext(FileContext fileContext, FileOrigin origin) {
        if (fileContext.isDuplicated()) {
            processDuplicatedFile(fileContext, origin);
            return;
        }

        processRejectFile(fileContext, origin);
    }

    private FileContext handleValidFileContext(FileContext fileContext, FileOrigin origin, Set<String> batchHashes) {
        String hash = fileContext.validationContext().getOrComputeHash();
        if (!batchHashes.add(hash)) {
            FileContext duplicatedContext = toDuplicatedContentContext(fileContext);
            processDuplicatedFile(duplicatedContext, origin);
            log.info("Duplicate hash detected within same batch. file={} hash={}", duplicatedContext.validationContext().getOriginalFilename(), hash);
            return duplicatedContext;
        }

        return persistOrMapAsDuplicated(fileContext, origin);
    }

    private FileContext persistOrMapAsDuplicated(FileContext fileContext, FileOrigin origin) {
        try {
            processNewFile(fileContext, origin);
            return fileContext;
        } catch (DuplicateHashPersistenceException ex) {
            FileContext duplicatedContext = toDuplicatedContentContext(fileContext);
            processDuplicatedFile(duplicatedContext, origin);
            log.info("Duplicate hash during persistence resolved as duplicated content. file={} hash={}",
                    duplicatedContext.validationContext().getOriginalFilename(),
                    duplicatedContext.validationContext().getOrComputeHash());
            return duplicatedContext;
        }
    }

    private FileContext toDuplicatedContentContext(FileContext fileContext) {
        return FileContext.fromWithDuplicatedContentStatus(
                fileContext.validationContext(),
                List.of(FailureReason.DUPLICATED_CONTENT),
                FailureReason.DUPLICATED_CONTENT
        );
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

    public FileHandlingResult processRejectFile(@NonNull FileContext fileContext, FileOrigin origin) {
        if (!fileContext.isInvalid())
            throw new IllegalArgumentException(fileContextMessageFactory.format(IngestionCommonMessageKey.FILE_REJECT_EXPECTS_INVALID_CONTEXT_ERROR, fileContext));

        FileHandlingResult afterStorageResult = fileStorageUtil.saveInDiskOverridingExisting(Paths.get(fileUploadProperties.rejectedPath()), fileContext);
        return this.outboxService.saveRejected(afterStorageResult, InternalServices.CHAIN_VALIDATION, origin);
    }

    public FileHandlingResult processNewFile(@NonNull FileContext fileContext, FileOrigin origin) {
        if (fileContext.isInvalid())
            throw new IllegalArgumentException(fileContextMessageFactory.format(IngestionCommonMessageKey.FILE_SAVE_NEW_EXPECTS_VALID_CONTEXT_ERROR, fileContext));

        FileHandlingResult afterStorageResult = fileStorageUtil.saveInDiskOverridingExisting(Paths.get(fileUploadProperties.pendingPath()), fileContext);
        return fileRecordService.saveNew(afterStorageResult, origin);
    }

    public FileHandlingResult processDuplicatedFile(@NonNull FileContext fileContext, FileOrigin origin) {
        if (fileContext.isDuplicated()) {
            FileHandlingResult afterStorageResult = fileStorageUtil.saveInDiskOverridingExisting(Paths.get(fileUploadProperties.duplicatesPath()), fileContext);
            return outboxService.saveDuplicated(afterStorageResult, InternalServices.CHAIN_VALIDATION, origin);
        }

        throw new IllegalArgumentException(fileContextMessageFactory.format(IngestionCommonMessageKey.FILE_SAVE_DUPLICATED_EXPECTS_DUPLICATED_CONTEXT_ERROR, fileContext));
    }

}
