package com.com4energy.processor.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.regex.Pattern;

import com.com4energy.processor.model.FailureReason;
import com.com4energy.processor.model.FileOrigin;
import com.com4energy.processor.model.FileRecord;
import com.com4energy.processor.model.FileStatus;
import com.com4energy.processor.service.FileRecordService;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;
import com.com4energy.processor.config.properties.FileUploadProperties;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@AllArgsConstructor
public final class FileStorageUtil {

    private final FileUploadProperties properties;
    private final FileRecordService fileRecordService;

    public Path moveFileToProcessed(File file) {
        return moveFile(file, properties.getProcessedPath() + "/" + defineSubFolder(file), "processed");
    }

    public Path moveFileToDuplicates(File file) {
        return moveFile(file, properties.getDuplicatesPath() + "/" + defineSubFolder(file), "duplicates");
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
        String originalFilename = file.getOriginalFilename();
        Path storagePath = Paths.get(pathToStorage, originalFilename);
        Files.createDirectories(storagePath.getParent());

        // Si el archivo ya existe en la base de datos
        if (this.fileRecordService.existsFileRecordByFilename(originalFilename)) {
            log.warn("Archivo ya registrado en base de datos: {}", originalFilename);

            // Generar nombre único basado en la base de datos
            String newFilename =  this.resolveDuplicateName(originalFilename);
            String subFolder = FilenameUtils.getExtension(storagePath.getFileName().toString()) + "/";

                    // Carpeta de duplicados
            Path duplicatesFolder = Path.of(this.properties.getDuplicatesPath() + "/" + subFolder);
            Files.createDirectories(duplicatesFolder);

            // Path final del archivo duplicado
            Path duplicatePath = duplicatesFolder.resolve(newFilename);

            // Guardar el archivo con el nuevo nombre
            Files.write(duplicatePath, file.getBytes());
            log.info("Archivo duplicado guardado como: {}", duplicatePath.toAbsolutePath());

            // Crear registro en la base de datos
            FileRecord fileRecord = new FileRecord();
            fileRecord.setFilename(newFilename);
            fileRecord.setHash(DigestUtils.md5DigestAsHex(file.getBytes()));
            fileRecord.setOriginPath(storagePath.toAbsolutePath().toString());
            fileRecord.setFinalPath(duplicatePath.toAbsolutePath().toString());
            fileRecord.setOrigin(FileOrigin.API);
            fileRecord.setFailureReason(FailureReason.DUPLICATE_FILE);
            fileRecord.setStatus(FileStatus.DUPLICATED);

            fileRecordService.saveFileRecord(fileRecord);

            return null;
        }

        // Si no existe en la BD, guardar normalmente
        Files.write(storagePath, file.getBytes());
        log.info("File {}, stored at: {}", originalFilename, storagePath.toAbsolutePath());

        // Registrar en BD
        //fileRecordService.saveFileRecord(originalFilename);

        return storagePath;
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
