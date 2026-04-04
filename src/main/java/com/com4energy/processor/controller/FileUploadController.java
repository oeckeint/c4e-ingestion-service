package com.com4energy.processor.controller;

import com.com4energy.i18n.core.Messages;
import com.com4energy.processor.common.IngestionCommonMessageKey;
import com.com4energy.processor.service.FileUploadOrchestratorService;
import com.com4energy.processor.service.dto.UploadBatchResult;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.com4energy.processor.api.response.ApiResponse;
import com.com4energy.processor.api.response.FileUploadBatchResponse;
import com.com4energy.processor.util.api.ResponseFilesFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/files")
public class FileUploadController {

    private final FileUploadOrchestratorService fileUploadOrchestratorService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<FileUploadBatchResponse>> uploadFiles(@NonNull @RequestParam("files") MultipartFile[] files ) {

        if (ArrayUtils.isEmpty(files))
            return ResponseFilesFactory.badRequest(
                    Messages.get(IngestionCommonMessageKey.UPLOAD_NO_FILES_PROVIDED));

        UploadBatchResult result = fileUploadOrchestratorService.processFiles(files);

        String message = Messages.format(
                IngestionCommonMessageKey.UPLOAD_BATCH_RESULT,
                result.uploadedData().size(),
                result.duplicates(),
                result.errors());

        return ResponseFilesFactory.accepted(message, new FileUploadBatchResponse(result.uploadedData()));
    }

}
