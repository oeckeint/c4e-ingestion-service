package com.com4energy.processor.model;

import java.time.LocalDateTime;
import java.util.Optional;

import com.com4energy.processor.service.dto.FileContext;
import lombok.*;
import jakarta.persistence.*;
import org.apache.commons.io.FilenameUtils;

@Entity
@Table(name = "file_records")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileRecord extends com.com4energy.processor.model.audit.Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String originalFilename;
    private String finalFilename;
    private String finalPath;

    private String extension;

    @Enumerated(EnumType.STRING)
    private FileType type;
    private String comment;

    @Enumerated(EnumType.STRING)
    private FileStatus status;

    @Enumerated(EnumType.STRING)
    private FileOrigin origin;

    private Integer retryCount;
    private String hash;

    @Enumerated(EnumType.STRING)
    private FailureReason failureReason;

    private LocalDateTime uploadedAt;
    private LocalDateTime processedAt;
    private LocalDateTime failedAt;
    private LocalDateTime lastAttemptAt;

    public String getExtension() {
        return FilenameUtils.getExtension(Optional.ofNullable
                        (this.originalFilename).orElse(this.extension));
    }

    public static FileRecord from(FileContext fileContext) {
        String finalFilename = Optional.of(fileContext.findStoredFilePath().orElse(""))
                .map(FilenameUtils::getName)
                .filter(name -> !name.isBlank())
                .orElseGet(() -> Optional.ofNullable(fileContext.validationContext().getOriginalFilename())
                        .filter(name -> !name.isBlank())
                        .orElseThrow(() -> new IllegalStateException(
                                "Cannot determine finalFilename for FileRecord.from; originalFilename and storedPath are empty"
                        )));

        return FileRecord.builder()
                .originalFilename(fileContext.validationContext().getOriginalFilename())
                .finalFilename(finalFilename)
                .finalPath(fileContext.findStoredFilePath().orElse(null))
                .hash(fileContext.validationContext().getOrComputeHash())
                .origin(FileOrigin.API)
                .retryCount(0)
                .build();
    }

    public void markAsFailed() {
        this.status = FileStatus.FAILED;
        this.failedAt = LocalDateTime.now();
    }

    //Despues combinaremos este con el AsRetry.. que sol intente retry y si se pasa de validaciones revuelve el failed
    public void markAsFailedWithComment(String comment) {
        this.status = FileStatus.FAILED;
        this.failedAt = LocalDateTime.now();
        this.comment = comment;
    }

    public void markAsNew() {
        this.status = FileStatus.NEW;
        this.uploadedAt = LocalDateTime.now();
    }

    public void markAsProcessing() {
        this.status = FileStatus.PROCESSING;
        this.lastAttemptAt = LocalDateTime.now();
    }

    public void markAsRejected() {
        this.status = FileStatus.REJECTED;
    }

    public void markAsDuplicatedOriginalFilename() {
        this.status = FileStatus.DUPLICATED_ORIGINAL_FILENAME;
    }

    public void markAsDuplicatedContent() {
        this.status = FileStatus.DUPLICATED_CONTENT;
    }

    public void markAsRetry(FailureReason reason) {
        this.failureReason = reason;
        this.status = FileStatus.RETRY;
        this.lastAttemptAt = LocalDateTime.now();
        this.retryCount = Optional.ofNullable(this.retryCount).orElse(0) + 1;
    }

    public void markAsSucceded() {
        this.status = FileStatus.SUCCEEDED;
        this.processedAt = LocalDateTime.now();
    }

}
