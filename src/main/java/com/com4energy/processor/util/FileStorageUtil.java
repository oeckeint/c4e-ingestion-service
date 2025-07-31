package com.com4energy.processor.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;

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
    public static Path storePendingFile(String pathToStorage, MultipartFile file) throws IOException {
        Path path = Paths.get(pathToStorage, file.getOriginalFilename());
        Files.createDirectories(path.getParent());
        Files.write(path, file.getBytes());
        return path;
    }

}