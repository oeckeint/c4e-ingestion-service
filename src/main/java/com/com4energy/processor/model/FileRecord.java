package com.com4energy.processor.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "file_records")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String filename;

    private String path;

    @Enumerated(EnumType.STRING)
    private FileStatus status;

    private LocalDateTime uploadedAt;

    private LocalDateTime processedAt;

    private LocalDateTime failedAt;

    private Integer retryCount;

    private LocalDateTime lastAttemptAt;
}
