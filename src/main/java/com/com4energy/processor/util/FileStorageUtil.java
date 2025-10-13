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

    private final HashUtils hashUtils;
    private final FileUploadProperties fileUploadProperties;
    private final FileRecordService fileRecordService;

    public Path moveFileFromAutomaticToProcessing(File file) {
        return moveFile(file, fileUploadProperties.getProcessingPath() + "/" + defineSubFolder(file), "processing");
    }

    public Path moveFileFromProcessingToProcessed(File file) {
        return moveFile(file, fileUploadProperties.getProcessedPath() + "/" + defineSubFolder(file), "processed");
    }

    public Path moveFileToDuplicates(File file) {
        return moveFile(file, fileUploadProperties.getDuplicatesPath() + "/" + defineSubFolder(file), "duplicates");
    }

    public Path moveFileToPending(File file) {
        return moveFile(file, fileUploadProperties.getPendingPath(), "pending");
    }

    public Path moveFileToFailed(File file) {
        return moveFile(file, fileUploadProperties.getFailedPath(), "failed");
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
        String originalFilename = Objects.requireNonNull(file.getOriginalFilename());
        Path storagePath = Paths.get(pathToStorage, originalFilename);
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
            Path duplicatesFolder = Path.of(this.fileUploadProperties.getDuplicatesPath(), subFolder);
            Files.createDirectories(duplicatesFolder);

            // Guardar archivo duplicado
            Path duplicatePath = duplicatesFolder.resolve(newFilename);
            Files.write(duplicatePath, file.getBytes());
            log.info("Archivo duplicado guardado como: {}", duplicatePath.toAbsolutePath());

            fileRecordService.saveFileRecord(
                    this.buidDuplicateFileRecord(
                            exisingFileRecord.get(),
                            pathToStorage + "/" + newFilename,
                            newFilename,
                            duplicatePath.toAbsolutePath().toString()
                    )
            );

            // Retornar null para indicar que no se debe procesar
            return null;
        }

        // Si no hay duplicado, guardar normalmente

        //fileRecord.setHash(fileHash);

        log.info("File {} stored at: {}", originalFilename, storagePath.toAbsolutePath());

        Files.write(storagePath, file.getBytes());
        return FileRecord.builder()
                //.finalPath(storagePath.toAbsolutePath().toString())
                .originPath(pathToStorage + "/" + originalFilename)
                .hash(fileHash)
                .build();
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

    private FileRecord buidDuplicateFileRecord(FileRecord exisingFileRecord, String originPath, String newFilename, String duplicatePath) {
        String comment = exisingFileRecord.getComment() == null ?
                null : exisingFileRecord.getComment() + " (duplicado)";
        return exisingFileRecord.toBuilder()
                .id(null)
                .filename(newFilename)
                .originPath(originPath)
                .finalPath(duplicatePath)
                .comment(comment)
                .status(FileStatus.DUPLICATED)
                .failureReason(FailureReason.DUPLICATE_FILE)
                .uploadedAt(LocalDateTime.now())
                .processedAt(LocalDateTime.now())
                .failedAt(LocalDateTime.now())
                .lastAttemptAt(LocalDateTime.now())
                .build();
    }

}
