package com.com4energy.processor.service;

import com.com4energy.i18n.core.Messages;
import com.com4energy.i18n.core.util.StringRules;
import com.com4energy.processor.api.response.FileMetadata;
import com.com4energy.processor.common.IngestionCommonMessageKey;
import com.com4energy.processor.config.properties.FileUploadProperties;
import com.com4energy.processor.model.FailureReason;
import com.com4energy.processor.model.FileOrigin;
import com.com4energy.processor.model.FileRecord;
import com.com4energy.processor.service.dto.FileRejectedRequest;
import com.com4energy.processor.service.dto.UploadBatchResult;
import com.com4energy.processor.util.FileStorageUtil;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.HtmlUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.com4energy.processor.util.FileUtils.extractExtension;
import static com.com4energy.processor.util.FileUtils.isSafeFilename;
import static com.com4energy.processor.util.FileUtils.isValidContentType;


@RequiredArgsConstructor
@Service
@Slf4j
public class FileUploadOrchestratorService {

    private record FileProcessResult(FileProcessStatus status, FileMetadata metadata) {}

    private final FileUploadProperties fileUploadProperties;
    private final FileRecordService fileRecordService;
    private final FileStorageUtil fileStorageUtil;
    private final Set<String> allowedExtensions;

    public UploadBatchResult processFiles(MultipartFile[] files) {
        if (isEmptyBatch(files)) {
            return buildEmptyBatchResult();
        }

        List<FileMetadata> uploadedFiles = new ArrayList<>();
        int duplicates = 0;
        int errors = 0;
        int processed = 0;
        int uploadedCount = 0;

        for (MultipartFile file : files) {
            processed++;
            Optional<FailureReason> failureReason = resolveValidationFailureReason(file);
            if (failureReason.isPresent()) {
                rejectFile(file, failureReason.get());
                errors++;
                continue;
            }

            try {
                FileProcessResult result = processSingleFile(file);

                switch (result.status()) {
                    case DUPLICATE -> {
                        duplicates++;
                        log.warn("Duplicate file detected: {}", file.getOriginalFilename());
                    }
                    case ERROR -> {
                        errors++;
                        log.error("Failed to process file after storage: {}", file.getOriginalFilename());
                    }
                    case UPLOADED -> {
                        uploadedCount++;
                        uploadedFiles.add(result.metadata());
                    }

                    case SKIPPED_EMPTY -> log.debug("Empty file skipped: {}", file.getOriginalFilename());

                }
            } catch (IOException e) {
                errors++;
                log.error("Error processing file: {} - IOException occurred during file storage or registration", file.getOriginalFilename(), e);
            } catch (Exception e) {
                errors++;
                log.error("Unexpected error processing file: {} - {}", file.getOriginalFilename(), e.getMessage(), e);
            }
        }

        UploadBatchResult uploadBatchResult = UploadBatchResult.builder()
                .duplicates(duplicates)
                .errors(errors)
                .processed(processed)
                .uploadedData(uploadedFiles)
                .uploadedCount(uploadedCount).build();

        log.info(Messages.format(IngestionCommonMessageKey.UPLOAD_BATCH_COMPLETED_LOG,
                uploadBatchResult.duplicates(), uploadBatchResult.errors(), uploadBatchResult.processed(), uploadBatchResult.uploadedData(), uploadBatchResult.uploadedCount()));

        return uploadBatchResult;
    }

    private @NotNull FileProcessResult processSingleFile(@NonNull MultipartFile file) throws IOException {

        FileRecord storedRecord = storePendingFileRecord(file);
        if (storedRecord == null) {
            return new FileProcessResult(FileProcessStatus.DUPLICATE, null);
        }

        String normalizedFilename = new File(storedRecord.getFinalPath()).getName();
        return registerAndPublish(normalizedFilename, storedRecord);
    }

    private FileRecord storePendingFileRecord(MultipartFile file) throws IOException {
        return fileStorageUtil.storeInPendingFilesFolder(fileUploadProperties.automaticPath(), file);
    }

    private FileProcessResult registerAndPublish(String filename,
                                                       @NonNull FileRecord storedRecord) {
        String originPath = storedRecord.getFinalPath();
        File currentFile = new File(originPath);

        FileRecord savedRecord = fileRecordService.registerFileAsPendingIntoDatabase(
                filename, originPath, FileOrigin.API, currentFile, storedRecord
        );

        if (savedRecord == null) {
            return new FileProcessResult(FileProcessStatus.ERROR, null);
        }

        FileMetadata metadata = new FileMetadata(
                HtmlUtils.htmlEscape(filename),
                HtmlUtils.htmlEscape(originPath)
        );
        return new FileProcessResult(FileProcessStatus.UPLOADED, metadata);
    }

//    private Optional<FailureReason> resolveValidationFailureReason(MultipartFile file) {
//        if (file == null) {
//            return Optional.of(FailureReason.NULL_FILE);
//        }
//
//        if (file.isEmpty()) {
//            return Optional.of(FailureReason.FILE_IS_EMPTY);
//        }
//
//        String filename = StringRules.trimToNull(file.getOriginalFilename());
//        if (filename == null) {
//            return Optional.of(FailureReason.INVALID_FILENAME);
//        }
//
//        if (fileRecordService.existsFileRecordByFilename(filename)) {
//            return Optional.of(FailureReason.DUPLICATED_FILENAME);
//        }
//
//        if (file.getSize() > fileUploadProperties.maxSizeBytes()) {
//            return Optional.of(FailureReason.FILE_TOO_LARGE);
//        }
//
//        if (!filename.contains(".")) {
//            return Optional.of(FailureReason.INVALID_EXTENSION);
//        }
//
//        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
//        boolean isAllowed = fileUploadProperties.allowedExtensions().stream()
//                .map(value -> value.toLowerCase(Locale.ROOT))
//                .anyMatch(ext::equals);
//        if (!isAllowed) {
//            return Optional.of(FailureReason.INVALID_EXTENSION);
//        }
//
//        return Optional.empty();
//    }

    private Optional<FailureReason> resolveValidationFailureReason(MultipartFile file) {
        if (file == null) {
            return Optional.of(FailureReason.NULL_FILE);
        }

        if (file.isEmpty()) {
            return Optional.of(FailureReason.FILE_IS_EMPTY);
        }

        if (file.getSize() > fileUploadProperties.maxSizeBytes()) {
            return Optional.of(FailureReason.FILE_TOO_LARGE);
        }

        String filename = StringRules.trimToNull(file.getOriginalFilename());
        if (filename == null) {
            return Optional.of(FailureReason.INVALID_FILENAME);
        }

        if (!isSafeFilename(filename)) {
            return Optional.of(FailureReason.INVALID_FILENAME);
        }

        String ext = extractExtension(filename);
        if (ext == null || !allowedExtensions.contains(ext)) {
            return Optional.of(FailureReason.INVALID_EXTENSION);
        }

        if (!isValidContentType(file)) {
            return Optional.of(FailureReason.INVALID_CONTENT_TYPE);
        }

        if (fileRecordService.existsFileRecordByFilename(filename)) {
            return Optional.of(FailureReason.DUPLICATED_FILENAME);
        }

        return Optional.empty();
    }


    private void rejectFile(MultipartFile file, FailureReason rejectionReason) {
        if (file != null) {
            FailureReason safeReason = Optional.ofNullable(rejectionReason).orElse(FailureReason.UNKNOWN_ERROR);
            String finalPath = fileStorageUtil.storeFailedFileAndResolveAbsolutePath(file).orElse(null);

            fileRecordService.saveRejected(
                    FileRejectedRequest.builder()
                            .filename(file.getOriginalFilename())
                            .finalPath(finalPath)
                            .reason(safeReason)
                            .build());
        }
    }

    private boolean isEmptyBatch(MultipartFile[] files) {
        return files == null || files.length == 0;
    }

    private UploadBatchResult buildEmptyBatchResult() {
        log.warn(Messages.get(IngestionCommonMessageKey.UPLOAD_EMPTY_FILES_ARRAY_LOG));
        return new UploadBatchResult(0, 0, 0, List.of(), 0);
    }

    private enum FileProcessStatus {
        SKIPPED_EMPTY,
        DUPLICATE,
        UPLOADED,
        ERROR
    }

}
