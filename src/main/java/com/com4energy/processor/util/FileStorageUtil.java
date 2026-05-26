package com.com4energy.processor.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

import com.com4energy.processor.model.FileStatus;
import com.com4energy.processor.service.dto.FileContext;
import com.com4energy.processor.service.dto.FileHandlingResult;
import com.com4energy.processor.service.validation.ValidationContext;
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
    private static final String TYPE_FOLDER_UNKNOWN = "unknown";
    private static final String TYPE_FOLDER_MEDIDA_H = "medida_h";
    private static final String TYPE_FOLDER_MEDIDA_QH = "medida_qh";
    private static final String TYPE_FOLDER_MEDIDA_CCH = "medida_cch";
    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM");

    private final FileUploadProperties fileUploadProperties;

    public Path moveFileFromAutomaticToProcessing(File file) {
        return moveFileToProcessing(file);
    }

    public Path moveFileToProcessing(File file) {
        return moveFile(file, fileUploadProperties.processingPath(), "processing");
    }

    public Path moveFileFromProcessingToProcessed(File file) {
        return moveFile(file, fileUploadProperties.processedPath(), "processed");
    }

    public Path moveFileToDuplicates(File file) {
        return moveFile(file, fileUploadProperties.duplicatesPath(), "duplicates");
    }

    public Path moveFileToPending(File file) {
        return moveFile(file, fileUploadProperties.pendingPath(), "pending");
    }

    public Path moveFileToFailed(File file) {
        return moveFile(file, fileUploadProperties.failedPath(), "failed");
    }

    public Path moveFileToRejected(File file) {
        return moveFile(file, fileUploadProperties.rejectedPath(), "rejected");
    }

    public Path moveFileToArchive(File file) {
        return moveFile(file, fileUploadProperties.archivePath(), "archive");
    }

    /**
     * Persists a {@link MultipartFile} directly into the configured failed folder.
     * If the originalFilename is blank or null the file is stored under {@value DEFAULT_FILENAME}.
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
            Path lifecyclePath = resolveLifecycleDirectory(basePath, safeFilename);
            Files.createDirectories(lifecyclePath);
            Path target = lifecyclePath.resolve(safeFilename).normalize();
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
     * Stores a MultipartFile in a base path with path traversal protection.
     * The originalFilename is inferred from {@link MultipartFile#getOriginalFilename()}.
     * Overwrites the file if it already exists.
     */
    public Path saveInDiskOverridingExisting(Path basePath, MultipartFile file) {
        return saveInDiskOverridingExisting(basePath, file, true);
    }

    /**
     * Rejection-oriented storage helper that returns the updated operational result.
     */
    public FileHandlingResult saveInDiskOverridingExisting(Path basePath, FileContext fileContext) {
        Path storedPath = saveInDiskOverridingExisting(basePath, fileContext.validationContext().getFile());
        if (storedPath == null) {
            return FileHandlingResult.initial(fileContext.withStatus(FileStatus.NOT_STORED));
        }
        return FileHandlingResult.initial(fileContext.withStoredPath(storedPath)).withStoredInDisk();
    }

    /**
     * Stores a MultipartFile in a base path with path traversal protection.
     * The originalFilename is inferred from {@link MultipartFile#getOriginalFilename()}.
     *
     * @param overrideExisting if {@code true} overwrites any existing file with the same name;
     *                         if {@code false} appends a numeric suffix, e.g. {@code originalFilename(01).xml}
     * @return stored path, or {@code null} if storage fails
     */
    public Path saveInDiskOverridingExisting(Path basePath, MultipartFile file, boolean overrideExisting) {
        try {
            Path normalizedBasePath = basePath.toAbsolutePath().normalize();
            String safeFilename = sanitizeFilename(resolveFilename(file));
            Path lifecyclePath = resolveLifecycleDirectory(normalizedBasePath, safeFilename);
            Path storagePath = lifecyclePath.resolve(safeFilename).normalize();

            if (!storagePath.startsWith(normalizedBasePath)) {
                throw new IOException("Invalid file path after sanitization");
            }

            if (!overrideExisting && Files.exists(storagePath)) {
                storagePath = resolveNonConflictingPath(lifecyclePath, safeFilename);
            }

            Files.createDirectories(storagePath.getParent());
            Files.write(storagePath, file.getBytes());
            return storagePath;
        } catch (IOException e) {
            log.error("Could not store file '{}' in disk path '{}': {}",
                    file != null ? file.getOriginalFilename() : null,
                    basePath,
                    e.getMessage(),
                    e);
            return null;
        }
    }

    /**
     * Deletes a file if it exists. Returns true when the cleanup completed without I/O errors.
     */
    public boolean deleteIfExists(Path path) {
        if (path == null) {
            return true;
        }

        try {
            Files.deleteIfExists(path);
            return true;
        } catch (IOException e) {
            log.error("Could not delete file '{}' during rollback: {}", path, e.getMessage(), e);
            return false;
        }
    }

    private Path resolveNonConflictingPath(Path basePath, String safeFilename) {
        String baseName = FilenameUtils.getBaseName(safeFilename);
        String extension = FilenameUtils.getExtension(safeFilename);
        String extensionSuffix = extension.isBlank() ? "" : "." + extension;

        int counter = 1;
        Path candidate;
        do {
            String numberedName = String.format("%s(%02d)%s", baseName, counter, extensionSuffix);
            candidate = basePath.resolve(numberedName).normalize();
            counter++;
        } while (Files.exists(candidate));

        return candidate;
    }

    private String resolveFilename(MultipartFile file) {
        String original = file != null ? file.getOriginalFilename() : null;
        return (original == null || original.isBlank()) ? DEFAULT_FILENAME : original;
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

    private Path moveFile(File file, String destinationDir, String label) {
        try {
            Path destinationRoot = Paths.get(destinationDir).toAbsolutePath().normalize();
            String safeFilename = sanitizeFilename(file.getName());
            Path lifecyclePath = resolveLifecycleDirectory(destinationRoot, safeFilename);
            Files.createDirectories(lifecyclePath);
            Path destinationPath = lifecyclePath.resolve(safeFilename).normalize();
            if (!destinationPath.startsWith(destinationRoot)) {
                throw new IOException("Invalid destination path after sanitization: " + file.getName());
            }
            if (Files.exists(destinationPath)) {
                destinationPath = resolveNonConflictingPath(lifecyclePath, destinationPath.getFileName().toString());
            }
            log.debug("Moving file to {}: {}", label, destinationPath.toAbsolutePath());
            return Files.move(file.toPath(), destinationPath);
        } catch (IOException e) {
            log.error("Error moving file to {}: {}", label, e.getMessage());
            return null;
        }
    }

    private Path resolveLifecycleDirectory(Path basePath, String filename) {
        String typeFolder = resolveTypeFolderFromFilename(filename);
        String yearMonthFolder = LocalDate.now().format(YEAR_MONTH_FORMATTER);
        return basePath.resolve(typeFolder).resolve(yearMonthFolder).normalize();
    }

    private String resolveTypeFolderFromFilename(String filename) {
        String extension = Optional.ofNullable(FilenameUtils.getExtension(filename))
                .map(ext -> ext.toLowerCase(Locale.ROOT))
                .orElse("");

        if (extension.length() != 1 || !Character.isDigit(extension.charAt(0))) {
            return TYPE_FOLDER_UNKNOWN;
        }

        String baseName = FilenameUtils.getBaseName(filename);
        String[] tokens = baseName.split("_");
        if (tokens.length == 0 || tokens[0].length() < 2) {
            return TYPE_FOLDER_UNKNOWN;
        }

        String prefix = tokens[0].substring(0, 2).toLowerCase(Locale.ROOT);
        return switch (prefix) {
            case "p1" -> TYPE_FOLDER_MEDIDA_H;
            case "p2" -> TYPE_FOLDER_MEDIDA_QH;
            case "f5" -> TYPE_FOLDER_MEDIDA_CCH;
            default -> TYPE_FOLDER_UNKNOWN;
        };
    }


    /**
     * Stores a {@link MultipartFile} using its {@code hash} as the storage artifact name,
     * without extension. If a file with that hash already exists on disk, the write is skipped
     * and the existing path is returned unchanged.
     * Returns empty when storing fails.
     */
    public Optional<Path> saveInDiskWithHash(Path basePath, ValidationContext validationContext) {
        try {
            Path normalizedBase = basePath.toAbsolutePath().normalize();
            Files.createDirectories(normalizedBase);

            String artifactName = validationContext.getOrComputeHash();
            MultipartFile file = validationContext.getFile();

            Path target = normalizedBase.resolve(artifactName).normalize();
            if (!target.startsWith(normalizedBase)) {
                throw new IOException("Path traversal detected for hash: " + artifactName);
            }

            if (Files.exists(target)) {
                log.info("file_storage=REUSED hash={} path={}", artifactName, target);
                return Optional.of(target);
            }

            Files.write(target, file.getBytes());
            log.info("file_storage=WRITTEN hash={} path={}", artifactName, target);
            return Optional.of(target);
        } catch (IOException e) {
            log.error("Could not store file with hash '{}' in '{}': {}", validationContext.getOrComputeHash(), basePath, e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Returns {@code true} when the hash artifact already exists in disk.
     */
    public boolean existsByHash(Path basePath, String hash) {
        if (hash == null || hash.isBlank()) {
            return false;
        }
        Path normalizedBase = basePath.toAbsolutePath().normalize();
        return Files.exists(normalizedBase.resolve(hash));
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
