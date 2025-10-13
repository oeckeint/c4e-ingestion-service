package com.com4energy.processor.controller;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.com4energy.processor.api.response.ApiResponse;
import com.com4energy.processor.api.response.FileMetadata;
import com.com4energy.processor.api.response.FileUploadBatchResponse;
import com.com4energy.processor.config.properties.FileUploadProperties;
import com.com4energy.processor.model.FileOrigin;
import com.com4energy.processor.model.FileRecord;
import com.com4energy.processor.service.FileRecordService;
import com.com4energy.processor.service.MessageProducer;
import com.com4energy.processor.util.FileStorageUtil;
import com.com4energy.processor.util.api.ResponseFilesFactory;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/files")
public class FileUploadController {

    private final MessageProducer messageProducer;
    private final FileUploadProperties fileUploadProperties;
    private final FileRecordService fileRecordService;
    private final FileStorageUtil fileStorageUtil;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<FileUploadBatchResponse>> uploadFiles(@RequestParam("files") MultipartFile[] files) throws IOException {
        if (files == null || files.length == 0) {
            return ResponseFilesFactory.badRequest("No files provided.");
        }

        List<FileMetadata> uploaded = new ArrayList<>();
        int duplicates = 0;
        int errors = 0;

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) continue;

            String filename = Objects.requireNonNull(file.getOriginalFilename());
            FileRecord fileRecord = this.fileStorageUtil.storeInPendingFilesFolder(this.fileUploadProperties.getAutomaticPath(), file);

            if (fileRecord == null) {
                duplicates++;
                continue;
            }

            String originPath = fileRecord.getOriginPath();
            File currentFile = new File(originPath);

            FileRecord savedRecord = this.fileRecordService.registerFileAsPendingIntoDatabase(filename, originPath, FileOrigin.API, currentFile, fileRecord);

            if (savedRecord != null) {
                messageProducer.sendFileAsMessageToRabbit(savedRecord);
                uploaded.add(new FileMetadata(filename, originPath));
            } else {
                errors++;
            }
        }

        String message = String.format(
                "%d file(s) uploaded successfully. %d duplicate(s) skipped. %d error(s).",
                uploaded.size(), duplicates, errors
        );
        return ResponseFilesFactory.accepted(message, new FileUploadBatchResponse(uploaded));
    }

}
