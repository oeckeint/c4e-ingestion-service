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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/files")
public class FileUploadController {

    private final FileUploadOrchestratorService fileUploadOrchestratorService;
    private final UploadBatchApiResponseMapper uploadBatchApiResponseMapper;

    // 0 o negativo = sin límite de cantidad por request.
    @Value("${c4e.upload.max-files-per-request}")
    private int maxFilesPerRequest;

    @Value("${c4e.upload.max-total-size-bytes}")
    private long maxTotalSizeBytes;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<FileUploadBatchResponse>> uploadFiles(@NonNull @RequestParam("files") MultipartFile[] files) {
        if (FileUtils.isEmptyBatch(files))
            return uploadBatchApiResponseMapper.toEmptyBatchResponse();

        if (maxFilesPerRequest > 0 && files.length > maxFilesPerRequest)
            return uploadBatchApiResponseMapper.toTooManyFilesResponse(maxFilesPerRequest);

        long totalSizeBytes = Arrays.stream(files).mapToLong(MultipartFile::getSize).sum();
        if (totalSizeBytes > maxTotalSizeBytes)
            return uploadBatchApiResponseMapper.toTotalSizeExceededResponse(maxTotalSizeBytes);

        FileBatchResult result = fileUploadOrchestratorService.processFiles(files);

        return uploadBatchApiResponseMapper.toAcceptedResponse(result);
    }

}
