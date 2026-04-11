package com.com4energy.processor.controller;

import com.com4energy.processor.controller.mapper.UploadBatchApiResponseMapper;
import com.com4energy.processor.service.FileUploadOrchestratorService;
import com.com4energy.processor.service.dto.FileBatchResult;
import com.com4energy.processor.api.response.ApiResponse;
import com.com4energy.processor.api.response.FileUploadBatchResponse;
import com.com4energy.processor.util.FileUtils;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/files")
public class FileUploadController {

    private final FileUploadOrchestratorService fileUploadOrchestratorService;
    private final UploadBatchApiResponseMapper uploadBatchApiResponseMapper;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<FileUploadBatchResponse>> uploadFiles(@NonNull @RequestParam("files") MultipartFile[] files) {
        if (FileUtils.isEmptyBatch(files))
            return uploadBatchApiResponseMapper.toEmptyBatchResponse();

        FileBatchResult result = fileUploadOrchestratorService.processFiles(files);

        return uploadBatchApiResponseMapper.toAcceptedResponse(result);
    }

}
