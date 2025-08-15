package com.com4energy.processor.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class FileStorageUtil {

    private FileStorageUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Stores a pending file in the specified storage path.
     *
     * @param pathToStorage the directory where the file will be stored
     * @param file          the MultipartFile to be stored
     * @return the Path of the stored file
     * @throws IOException if an I/O error occurs
     */
    public static Path storeInPendingFilesFolder(String pathToStorage, MultipartFile file) throws IOException {
        Path path = Paths.get(pathToStorage, file.getOriginalFilename());
        Files.createDirectories(path.getParent());
        if (Files.exists(path)) {
            log.warn("⚠️ Archivo ya existente. No se sobrescribe: {}", path.toAbsolutePath());
            return null;
        }
        Files.write(path, file.getBytes());
        log.info("File {}, stored at: {}", file.getOriginalFilename(), path.toAbsolutePath());
        return path;
    }

}
