package com.com4energy.processor.controller;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.com4energy.processor.api.ApiMessages;
import com.com4energy.processor.api.response.ApiResponse;
import com.com4energy.processor.api.response.FileMetadata;
import com.com4energy.processor.api.response.FileUploadResponse;
import com.com4energy.processor.api.response.FileUploadBatchResponse;
import com.com4energy.processor.config.properties.FileUploadProperties;
import com.com4energy.processor.model.FileOrigin;
import com.com4energy.processor.model.FileRecord;
import com.com4energy.processor.service.FileRecordService;
import com.com4energy.processor.service.MessageProducer;
import com.com4energy.processor.util.FileStorageUtil;
import com.com4energy.processor.util.api.ResponseFilesFactory;
import lombok.extern.slf4j.Slf4j;
import static java.util.Objects.isNull;

@Slf4j
@RestController
@RequestMapping("/files")
public class FileUploadController {

    private final MessageProducer messageProducer;
    private final FileUploadProperties fileUploadProperties;
    private final FileRecordService fileRecordService;
    private final FileStorageUtil fileStorageUtil;

    public FileUploadController(MessageProducer messageProducer, FileUploadProperties fileUploadProperties, FileRecordService fileRecordService, FileStorageUtil fileStorageUtil) {
        this.messageProducer = messageProducer;
        this.fileUploadProperties = fileUploadProperties;
        this.fileRecordService = fileRecordService;
        this.fileStorageUtil = fileStorageUtil;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        String filename = Objects.requireNonNull(file.getOriginalFilename());

        Path storedPath = this.fileStorageUtil.storeInPendingFilesFolder(this.fileUploadProperties.getPendingPath(), file);
        if (isNull(storedPath)) return ResponseFilesFactory.conflict(ApiMessages.FILE_ALREADY_EXISTS);

        String path = storedPath.toString();
        FileRecord record = this.fileRecordService.registerFileAsPendingIntoDatababase(filename, path, FileOrigin.API);
        if (isNull(record)) return ResponseFilesFactory.conflict(ApiMessages.FILE_ALREADY_EXISTS);

        this.messageProducer.sendFileAsMessageToRabbit(record);

        FileUploadResponse response = new FileUploadResponse(new FileMetadata(filename, path));
        return ResponseFilesFactory.accepted(ApiMessages.FILE_UPLOADED_SUCCESSFULLY, response);
    }

    @PostMapping(value = "/upload/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<FileUploadBatchResponse>> uploadFiles(@RequestParam("files") MultipartFile[] files) throws IOException {
        List<FileMetadata> uploaded = new ArrayList<>();
        int duplicates = 0;

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            String filename = Objects.requireNonNull(file.getOriginalFilename());
            Path storedPath = this.fileStorageUtil.storeInPendingFilesFolder(this.fileUploadProperties.getAutomaticPath(), file);
            if (isNull(storedPath)) {
                duplicates++;
                continue;
            }
            String path = storedPath.toString();
            FileRecord record = this.fileRecordService.registerFileAsPendingIntoDatababase(filename, path, FileOrigin.API);
            if (isNull(record)) {
                duplicates++;
                continue;
            }
            this.messageProducer.sendFileAsMessageToRabbit(record);
            uploaded.add(new FileMetadata(filename, path));
        }

        FileUploadBatchResponse response = new FileUploadBatchResponse(uploaded);
        String message = String.format("%d file(s) uploaded successfully. %d duplicate(s) skipped.", uploaded.size(), duplicates);
        return ResponseFilesFactory.accepted(message, response);
    }

}
