package com.com4energy.processor.controller;

import com.com4energy.processor.api.ApiMessages;
import com.com4energy.processor.api.ApiStatus;
import com.com4energy.processor.config.properties.FileStorageProperties;
import com.com4energy.processor.api.response.ApiResponse;
import com.com4energy.processor.api.response.FileMetadata;
import com.com4energy.processor.api.response.FileUploadResponse;
import com.com4energy.processor.model.FileRecord;
import com.com4energy.processor.service.FileRecordService;
import com.com4energy.processor.service.MessageProducer;
import com.com4energy.processor.util.FileStorageUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Objects;

@RestController
@RequestMapping("/files")
public class FileUploadController {

    private final MessageProducer messageProducer;
    private final FileStorageProperties fileStorageProperties;
    private final FileRecordService fileRecordService;

    public FileUploadController(MessageProducer messageProducer, FileStorageProperties fileStorageProperties, FileRecordService fileRecordService) {
        this.messageProducer = messageProducer;
        this.fileStorageProperties = fileStorageProperties;
        this.fileRecordService = fileRecordService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        String filename = Objects.requireNonNull(file.getOriginalFilename());
        String path = FileStorageUtil.storePendingFile(fileStorageProperties.getUploadPath(), file).toString();

        FileRecord record = fileRecordService.registerFile(filename, path);

        if (record == null) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiResponse<>(
                        ApiStatus.WARNING,
                        ApiMessages.FILE_ALREADY_EXISTS,
                        null
                ));
        }

        messageProducer.sendFileMessage(record);

        FileUploadResponse response = new FileUploadResponse(new FileMetadata(filename, path));

        return ResponseEntity.accepted()
            .body(new ApiResponse<>(
                    ApiStatus.SUCCESS,
                    ApiMessages.FILE_UPLOADED_SUCCESSFULLY,
                    response
            ));
    }

}
