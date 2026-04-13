package com.com4energy.processor.model;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

import com.com4energy.processor.service.dto.FileContext;
import lombok.*;
import jakarta.persistence.*;
import org.apache.commons.io.FilenameUtils;

@Entity
@Table(name = "file_records")
@Getter
@Setter
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
    private QualityStatus qualityStatus;

    @Enumerated(EnumType.STRING)
    private BusinessResult businessResult;

    @Enumerated(EnumType.STRING)
    private FileOrigin origin;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean locked;

    private String lockedBy;

    private LocalDateTime lockedAt;

    private Integer retryCount;
    private Integer processedRecords;
    private Integer defectedRecords;
    private Long parseDurationMs;
    private Long processingDurationMs;
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
        return from(fileContext, FileOrigin.API);
    }

    public static FileRecord from(FileContext fileContext, FileOrigin origin) {
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
                .type(determineInitialType(fileContext.validationContext().getOriginalFilename()))
                .hash(fileContext.validationContext().getOrComputeHash())
                .origin(origin)
                .qualityStatus(QualityStatus.NOT_EVALUATED)
                .businessResult(BusinessResult.NOT_PROCESSED)
                .retryCount(0)
                .build();
    }

    private static FileType determineInitialType(String originalFilename) {
        String extension = Optional.ofNullable(FilenameUtils.getExtension(originalFilename))
                .map(ext -> ext.toLowerCase(Locale.ROOT))
                .orElse("");

        if ("xml".equals(extension)) {
            return FileType.AWAITING_CLASSIFICATION;
        }

        if (extension.length() == 1 && Character.isDigit(extension.charAt(0))) {
            return resolveMeasureTypeFromFilename(originalFilename);
        }

        return FileType.UNKNOWN;
    }

    private static FileType resolveMeasureTypeFromFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return FileType.UNKNOWN;
        }

        String baseName = FilenameUtils.getBaseName(originalFilename);
        String[] tokens = baseName.split("_");
        if (tokens.length == 0 || tokens[0].length() < 2) {
            return FileType.UNKNOWN;
        }

        String prefix = tokens[0].substring(0, 2).toLowerCase(Locale.ROOT);
        return switch (prefix) {
            case "p1" -> FileType.MEDIDA_QH_P1;
            case "p2" -> FileType.MEDIDA_QH_P2;
            case "f5" -> FileType.MEDIDA_QH_F5;
            case "p5" -> FileType.MEDIDA_QH_P5;
            default -> FileType.UNKNOWN;
        };
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

    public void markAsPending() {
        this.status = FileStatus.PENDING;
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
