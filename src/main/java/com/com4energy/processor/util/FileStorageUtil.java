package com.com4energy.processor.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import com.com4energy.processor.model.FailureReason;
import com.com4energy.processor.model.FileRecord;
import com.com4energy.processor.model.FileStatus;
import com.com4energy.processor.service.FileRecordService;
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

    private static final int MAX_FILENAME_LENGTH = 255;
    private static final String DEFAULT_FILENAME = "file";
    private static final String SAFE_FILENAME_REGEX = "[^A-Za-z0-9._-]";

    private final HashUtils hashUtils;
    private final FileUploadProperties fileUploadProperties;
    private final FileRecordService fileRecordService;

    public Path moveFileFromAutomaticToProcessing(File file) {
        return moveFile(file, fileUploadProperties.processingPath() + "/" + defineSubFolder(file), "processing");
    }

    public Path moveFileFromProcessingToProcessed(File file) {
        return moveFile(file, fileUploadProperties.processedPath() + "/" + defineSubFolder(file), "processed");
    }

    public Path moveFileToDuplicates(File file) {
        return moveFile(file, fileUploadProperties.duplicatesPath() + "/" + defineSubFolder(file), "duplicates");
    }

    public Path moveFileToPending(File file) {
        return moveFile(file, fileUploadProperties.pendingPath(), "pending");
    }

    public Path moveFileToFailed(File file) {
        return moveFile(file, fileUploadProperties.failedPath(), "failed");
    }

    /**
     * Persists a {@link MultipartFile} directly into the configured failed folder.
     * If the filename is blank or null the file is stored under {@value DEFAULT_FILENAME}.
     *
     * @param file the uploaded file that failed validation
     * @return the absolute {@link Path} where the file was written, or {@code null} on I/O error
     */
    public Path storeInFailedFolder(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String safeFilename = (originalFilename == null || originalFilename.isBlank())
                ? DEFAULT_FILENAME
                : sanitizeFilename(originalFilename);

        try {
            Path basePath = Paths.get(fileUploadProperties.failedPath()).toAbsolutePath().normalize();
            Files.createDirectories(basePath);
            Path target = basePath.resolve(safeFilename).normalize();
            if (!target.startsWith(basePath)) {
                throw new IOException("Invalid file path after sanitization: " + safeFilename);
            }
            Files.write(target, file.getBytes());
            log.info("Invalid file stored in failed folder: {}", target);
            return target;
        } catch (IOException e) {
            log.error("Could not persist invalid file '{}' to failed folder: {}", safeFilename, e.getMessage());
            return null;
        }
    }

    /**
     * Stores a pending file in the specified storage path.
     *
     * @param pathToStorage the directory where the file will be stored
     * @param file          the MultipartFile to be stored
     * @return the Path of the stored file
     * @throws IOException if an I/O error occurs
     */
    public FileRecord storeInPendingFilesFolder(String pathToStorage, MultipartFile file) throws IOException {
        String originalFilename = sanitizeFilename(Objects.requireNonNull(file.getOriginalFilename()));
        Path basePath = Paths.get(pathToStorage).toAbsolutePath().normalize();
        Path storagePath = basePath.resolve(originalFilename).normalize();
        if (!storagePath.startsWith(basePath)) {
            throw new IOException("Invalid file path after sanitization");
        }
        Files.createDirectories(storagePath.getParent());

        // Calcular hash opcionalmente
        String fileHash = this.hashUtils.computeHashIfEnabled(file);

        // Chequear duplicado en base de datos por nombre o hash
        Optional<FileRecord> exisingFileRecord = this.fileRecordService.findFirstByFilenameOrHash(originalFilename, fileHash);
        if (exisingFileRecord.isPresent()) {
            log.warn("Archivo ya registrado en base de datos: {}", originalFilename);

            // Generar nombre único para duplicado
            String newFilename = this.resolveDuplicateName(originalFilename);
            String subFolder = FilenameUtils.getExtension(storagePath.getFileName().toString()) + "/";
            Path duplicatesBasePath = Path.of(this.fileUploadProperties.duplicatesPath(), subFolder)
                    .toAbsolutePath()
                    .normalize();
            Files.createDirectories(duplicatesBasePath);

            // Guardar archivo duplicado
            Path duplicatePath = duplicatesBasePath.resolve(newFilename).normalize();
            if (!duplicatePath.startsWith(duplicatesBasePath)) {
                throw new IOException("Invalid duplicate file path after sanitization");
            }
            Files.write(duplicatePath, file.getBytes());
            log.info("Archivo duplicado guardado como: {}", duplicatePath.toAbsolutePath());

            fileRecordService.save(
                    this.buidDuplicateFileRecord(
                            exisingFileRecord.get(),
                            basePath.resolve(newFilename).toString(),
                            newFilename,
                            duplicatePath.toAbsolutePath().toString()
                    )
            );

            // Retornar null para indicar que no se debe procesar
            return null;
        }

        // Si no hay duplicado, guardar normalmente

        log.info("File {} stored at: {}", originalFilename, storagePath.toAbsolutePath());

        Files.write(storagePath, file.getBytes());
        return FileRecord.builder()
                //.finalPath(storagePath.toAbsolutePath().toString())
                .finalPath(storagePath.toString())
                .hash(fileHash)
                .build();
    }

    private String sanitizeFilename(String filename) {
        String basename = FilenameUtils.getName(filename);
        String sanitized = basename.replaceAll(SAFE_FILENAME_REGEX, "_").trim();
        if (sanitized.isBlank()) {
            return DEFAULT_FILENAME;
        }
        if (sanitized.length() <= MAX_FILENAME_LENGTH) {
            return sanitized;
        }

        String extension = FilenameUtils.getExtension(sanitized);
        String base = FilenameUtils.getBaseName(sanitized);
        int allowedBaseLength = extension.isBlank()
                ? MAX_FILENAME_LENGTH
                : MAX_FILENAME_LENGTH - extension.length() - 1;
        if (allowedBaseLength <= 0) {
            return sanitized.substring(0, MAX_FILENAME_LENGTH);
        }
        String truncatedBase = base.substring(0, Math.min(base.length(), allowedBaseLength));
        return extension.isBlank() ? truncatedBase : truncatedBase + "." + extension;
    }

    public String resolveDuplicateName(String originalFilename) {
        String baseName = FilenameUtils.getBaseName(originalFilename);
        String extension = FilenameUtils.getExtension(originalFilename);

        // Obtener todos los filenames que empiezan con el mismo baseName
        List<String> similarNames = this.fileRecordService.findAllFilenamesLike(baseName + "%");

        int maxCounter = 0;
        for (String name : similarNames) {
            // Extraer número si tiene formato archivo(n).ext
            if (name.matches(Pattern.quote(baseName) + "\\(\\d+\\)\\." + Pattern.quote(extension))) {
                int num = Integer.parseInt(name.replaceAll(".*\\((\\d+)\\)\\..*", "$1"));
                if (num > maxCounter) maxCounter = num;
            }
        }

        // Generar nuevo nombre con contador siguiente
        int nextCounter = maxCounter + 1;
        return baseName + "(" + nextCounter + ")." + extension;
    }

    private Path moveFile(File file, String destinationDir, String label) {
        try {
            //Debemos definir un subtipo de carpeta para los tipos de medida
            Files.createDirectories(Paths.get(destinationDir));
            Path destinationPath = Paths.get(destinationDir, file.getName());
            log.info("Moving file to {}: {}", label, destinationPath.toAbsolutePath());
            return Files.move(file.toPath(), destinationPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Error moving file to {}: {}", label, e.getMessage());
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

    private FileRecord buidDuplicateFileRecord(FileRecord exisingFileRecord, String originPath, String newFilename, String duplicatePath) {
        String comment = exisingFileRecord.getComment() == null ?
                null : exisingFileRecord.getComment() + " (duplicado)";
        return exisingFileRecord.toBuilder()
                .id(null)
                .filename(newFilename)
                .finalPath(duplicatePath)
                .comment(comment)
                .status(FileStatus.DUPLICATED)
                .failureReason(FailureReason.DUPLICATED_FILE)
                .uploadedAt(LocalDateTime.now())
                .processedAt(LocalDateTime.now())
                .failedAt(LocalDateTime.now())
                .lastAttemptAt(LocalDateTime.now())
                .build();
    }

    /**
     * Stores an invalid file and returns its absolute path as String when successful.
     * Returns empty if the file is null or could not be stored.
     */
    public Optional<String> storeFailedFileAndResolveAbsolutePath(MultipartFile file) {
        if (file == null) {
            log.warn("Cannot store invalid file because MultipartFile is null");
            return Optional.empty();
        }

        Path storedPath = storeInFailedFolder(file);
        if (storedPath == null) {
            log.warn("Failed to store invalid file '{}' in failed folder", file.getOriginalFilename());
            return Optional.empty();
        }

        return Optional.of(storedPath.toAbsolutePath().toString());
    }

}
