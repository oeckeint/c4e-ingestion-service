package com.com4energy.processor.controller.mapper;

import com.com4energy.i18n.core.Messages;
import com.com4energy.processor.api.response.ApiResponse;
import com.com4energy.processor.api.response.FileBatchItemResponse;
import com.com4energy.processor.api.response.FileUploadBatchResponse;
import com.com4energy.processor.common.IngestionCommonMessageKey;
import com.com4energy.processor.service.dto.FileContext;
import com.com4energy.processor.service.dto.FileBatchResult;
import com.com4energy.processor.util.api.ResponseFilesFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UploadBatchApiResponseMapper {

    public ResponseEntity<ApiResponse<FileUploadBatchResponse>> toEmptyBatchResponse() {
        return ResponseFilesFactory.badRequest(Messages.get(IngestionCommonMessageKey.UPLOAD_NO_FILES_PROVIDED));
    }

    public ResponseEntity<ApiResponse<FileUploadBatchResponse>> toAcceptedResponse(FileBatchResult result) {
        return ResponseFilesFactory.accepted(toBatchMessage(result), toBatchResponse(result));
    }

    public FileUploadBatchResponse toBatchResponse(FileBatchResult result) {
        return new FileUploadBatchResponse(
                toItems(result.validFiles()),
                toItems(result.duplicatedFiles()),
                toItems(result.failedFiles())
        );
    }

    public String toBatchMessage(FileBatchResult result) {
        return Messages.format(IngestionCommonMessageKey.UPLOAD_BATCH_RESULT,
                result.successCount(), result.alreadyExistsCount(), result.errors());
    }

    public List<FileBatchItemResponse> toItems(List<FileContext> fileContexts) {
        return fileContexts.stream()
                .map(this::toBatchItemResponse)
                .toList();
    }

    public FileBatchItemResponse toBatchItemResponse(FileContext fileContext) {
        String filename = fileContext.findStoredFilePath().orElse(fileContext.validationContext().getOriginalFilename());
        return new FileBatchItemResponse(
                fileContext.validationContext().getOriginalFilename(),
                filename,
                fileContext.fileStatus()
        );
    }

}
