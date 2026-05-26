package com.com4energy.processor.repository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import com.com4energy.processor.model.FileType;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.com4energy.processor.model.FileRecord;
import com.com4energy.processor.model.FileStatus;

/**
 * Repository interface for managing FileRecord entities.
 * Provides methods to find FileRecords by originalFilename and path, and by status.
 */
@Repository
public interface
FileRecordRepository extends JpaRepository<FileRecord, Long> {

    boolean existsByOriginalFilenameAndFinalPath(String originalFilename, String finalPath);

    boolean existsByHash(String hash);

    boolean existsByOriginalFilename(String originalFilename);

    Optional<FileRecord> findFirstByOriginalFilename(String originalFilename);

    @Query("SELECT f.originalFilename FROM FileRecord f WHERE f.originalFilename LIKE :pattern")
    List<String> findAllOriginalFilenamesLike(@Param("pattern") String pattern);

    /**
     * Returns true if any file in the same "family" (same base name, any extension)
     * is currently locked (being processed), excluding the file itself.
     *
     * Used to defer processing of a revision when a sibling revision is already in-flight,
     * preventing concurrent writes to the same measure records without requiring UNIQUE constraints.
     */
    @Query("""
            SELECT COUNT(f) > 0 FROM FileRecord f
            WHERE f.originalFilename LIKE :familyPattern
              AND f.id <> :excludeId
              AND f.locked = true
              AND f.status = :processingStatus
            """)
    boolean existsFamilyFileBeingProcessed(
            @Param("familyPattern") String familyPattern,
            @Param("excludeId") Long excludeId,
            @Param("processingStatus") FileStatus processingStatus
    );

    Optional<FileRecord> findByOriginalFilenameAndFinalPath(String originalFilename, String finalPath);

    Optional<FileRecord> findByOriginalFilenameAndFinalPathOrHash(String originalFilename, String finalPath, String fileHash);

    Optional<FileRecord> findByHash(String hash);

    List<FileRecord> findByStatus(FileStatus status);

    List<FileRecord> findByStatusIn(List<FileStatus> statuses);

    List<FileRecord> findByLockedTrueAndLockedAtBefore(LocalDateTime threshold);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select f
            from FileRecord f
            where f.status in :statuses
              and (f.locked = false or f.locked is null)
              and f.type in :types
            order by f.uploadedAt asc, f.id asc
            """)
    List<FileRecord> findCandidatesForProcessing(
            @Param("statuses") List<FileStatus> statuses,
            @Param("types") List<FileType> types,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select f
            from FileRecord f
            where f.id = :id
            """)
    Optional<FileRecord> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select f
            from FileRecord f
            where f.id = :id
              and f.locked = true
              and f.lockedBy = :lockedBy
            """)
    Optional<FileRecord> findOwnedByIdForUpdate(@Param("id") Long id, @Param("lockedBy") String lockedBy);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update FileRecord f
            set f.locked = false,
                f.lockedBy = null,
                f.lockedAt = null
            where f.id = :id
              and f.locked = true
              and f.lockedBy = :lockedBy
            """)
    int releaseLockIfOwnedBy(@Param("id") Long id, @Param("lockedBy") String lockedBy);

}
