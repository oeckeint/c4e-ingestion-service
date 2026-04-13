package com.com4energy.processor.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
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

    Optional<FileRecord> findByOriginalFilenameAndFinalPath(String originalFilename, String finalPath);

    Optional<FileRecord> findByOriginalFilenameAndFinalPathOrHash(String originalFilename, String finalPath, String fileHash);

    Optional<FileRecord> findByHash(String hash);

    List<FileRecord> findByStatus(FileStatus status);

    List<FileRecord> findByStatusIn(List<FileStatus> statuses);

}
