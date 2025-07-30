package com.com4energy.processor.controller;

import com.com4energy.processor.service.FileProcessingService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/files")
public class FileUploadController {

    private final FileProcessingService processingService;

    public FileUploadController(FileProcessingService processingService) {
        this.processingService = processingService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        Path path = Paths.get("/Users/jesus/Downloads/uploads/pending", file.getOriginalFilename());
        Files.createDirectories(path.getParent());
        Files.write(path, file.getBytes());

        // Enviar el path a procesamiento asíncrono
        processingService.processFile(path.toFile());

        return ResponseEntity.accepted().body("Archivo recibido y en proceso.");
    }
}
