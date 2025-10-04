package com.com4energy.processor.model;

import java.time.LocalDateTime;
import lombok.*;
import jakarta.persistence.*;

@Entity
@Table(name = "file_records")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileRecord extends com.com4energy.processor.model.audit.Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String filename;
    private String originPath;
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

}
