package com.com4energy.processor.service.dto;

import com.com4energy.processor.api.response.FileMetadata;
import lombok.Builder;

import java.util.ArrayList;
import java.util.List;

@Builder
public record FileBatchResult(
        List<FileMetadata> uploadedData,
        List<FileContext> failedFiles,
        List<FileContext> duplicatedFiles,
        List<FileContext> validFiles) {

    public static FileBatchResult from(
            List<FileMetadata> uploadedData,
            List<FileContext> failedFiles,
            List<FileContext> duplicatedFiles,
            List<FileContext> validFiles) {
        return new FileBatchResult(
                safeList(uploadedData),
                safeList(failedFiles),
                safeList(duplicatedFiles),
                safeList(validFiles)
        );
    }

    public static FileBatchResult empty() {
        return from(null, null, null, null);
    }

    public static FileBatchResult fromFileContexts(List<FileContext> fileContexts) {
        if (fileContexts == null || fileContexts.isEmpty()) {
            return empty();
        }

        Accumulator batch = new Accumulator();
        fileContexts.forEach(batch::add);
        return batch.toResult();
    }

    public int errors() {
        return safeSize(failedFiles);
    }

    public int processed() {
        return safeSize(validFiles) + safeSize(duplicatedFiles) + safeSize(failedFiles);
    }

    public int alreadyExistsCount() {
        return safeSize(duplicatedFiles);
    }

    public int successCount() {
        return safeSize(validFiles) + safeSize(duplicatedFiles);
    }

    private static int safeSize(List<?> items) {
        return items == null ? 0 : items.size();
    }

    private static <T> List<T> safeList(List<T> items) {
        return items == null ? List.of() : List.copyOf(items);
    }

    private static class Accumulator {
        private final List<FileMetadata> uploadedData = new ArrayList<>();
        private final List<FileContext> failedFiles = new ArrayList<>();
        private final List<FileContext> duplicatedFiles = new ArrayList<>();
        private final List<FileContext> validFiles = new ArrayList<>();

        public void add(FileContext fileContext) {
            uploadedData.add(fileContext.toFileMetadata());
            switch (fileContext.fileStatus()) {
                case VALID -> validFiles.add(fileContext);
                case DUPLICATED_ORIGINAL_FILENAME, DUPLICATED_CONTENT -> duplicatedFiles.add(fileContext);
                default -> failedFiles.add(fileContext);
            }
        }

        public FileBatchResult toResult() {
            return FileBatchResult.from(uploadedData, failedFiles, duplicatedFiles, validFiles);
        }
    }

}
