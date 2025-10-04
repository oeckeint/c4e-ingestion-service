package com.com4energy.processor.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import com.com4energy.processor.config.properties.FileUploadProperties;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@AllArgsConstructor
public final class FileStorageUtil {

    private final FileUploadProperties properties;

    public Path moveFileToProcessed(File file) {
        return moveFile(file, properties.getProcessedPath() + "/" + defineSubFolder(file), "processed");
    }

    public Path moveFileToDuplicates(File file) {
        return moveFile(file, properties.getDuplicatesPath(), "duplicates");
    }

    public Path moveFileToPending(File file) {
        return moveFile(file, properties.getPendingPath(), "pending");
    }

    public Path moveFileToFailed(File file) {
        return moveFile(file, properties.getFailedPath(), "failed");
    }

    /**
     * Stores a pending file in the specified storage path.
     *
     * @param pathToStorage the directory where the file will be stored
     * @param file          the MultipartFile to be stored
     * @return the Path of the stored file
     * @throws IOException if an I/O error occurs
     */
    public Path storeInPendingFilesFolder(String pathToStorage, MultipartFile file) throws IOException {
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

    private Path resolveConflict(Path path) throws IOException {
        int counter = 1;
        Path newPath = path;
        while (Files.exists(newPath)) {
            String fileName = FilenameUtils.getBaseName(path.getFileName().toString());
            String extension = FilenameUtils.getExtension(path.getFileName().toString());
            String newName = fileName + "(" + counter++ + ")." + extension;
            newPath = path.getParent().resolve(newName);
        }
        return newPath;
    }

    private Path moveFile(File file, String destinationDir, String label) {
        try {
            Files.createDirectories(Paths.get(destinationDir));
            Path destinationPath = Paths.get(destinationDir, file.getName());
            Files.move(file.toPath(), destinationPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("File moved to {}: {}", label, destinationPath.toAbsolutePath());
            return destinationPath;
        } catch (IOException e) {
            log.error("Error moving file to {}: {}", label, e.getMessage());
            e.printStackTrace(System.err);
            return null;
        }
    }

    private String defineSubFolder(File file) {
        String extension = FilenameUtils.getExtension(file.getName()).toLowerCase();
        return switch (extension) {
            case "pdf" -> "pdf/";
            case "xml" -> "xml/";
            default -> "others/";
        };
    }

}
