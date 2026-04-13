package com.com4energy.processor.service.dto;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class PathMultipartFile implements MultipartFile {

    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    private final Path path;
    private final String originalFilename;
    private final String contentType;
    private final long size;

    private PathMultipartFile(Path path, String originalFilename, String contentType, long size) {
        this.path = path;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.size = size;
    }

    public static PathMultipartFile fromPath(Path path) throws IOException {
        String filename = path.getFileName() == null ? "file" : path.getFileName().toString();
        String detectedContentType = Files.probeContentType(path);
        if (detectedContentType == null || detectedContentType.isBlank()) {
            detectedContentType = DEFAULT_CONTENT_TYPE;
        }
        long fileSize = Files.size(path);
        return new PathMultipartFile(path, filename, detectedContentType, fileSize);
    }

    @Override
    public String getName() {
        return originalFilename;
    }

    @Override
    public String getOriginalFilename() {
        return originalFilename;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public byte[] getBytes() throws IOException {
        return Files.readAllBytes(path);
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return Files.newInputStream(path);
    }

    @Override
    public void transferTo(File dest) throws IOException {
        Files.copy(path, dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }
}

