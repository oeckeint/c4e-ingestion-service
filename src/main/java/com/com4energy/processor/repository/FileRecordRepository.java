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
 * Provides methods to find FileRecords by filename and path, and by status.
 */
@Repository
public interface
FileRecordRepository extends JpaRepository<FileRecord, Long> {

    boolean existsByFilenameAndFinalPath(String filename, String finalPath);

    boolean existsFileRecordByFilename(String filename);

    Optional<FileRecord> findFirstByFilenameOrHash(String originalFilename, String fileHash);

    @Query("SELECT f.filename FROM FileRecord f WHERE f.filename LIKE :pattern")
    List<String> findAllFilenamesLike(@Param("pattern") String pattern);

    Optional<FileRecord> findByFilenameAndFinalPath(String filename, String finalPath);

    Optional<FileRecord> findByFilenameAndFinalPathOrHash(String filename, String finalPath, String fileHash);

    Optional<FileRecord> findByHash(String hash);

    List<FileRecord> findByStatus(FileStatus status);

    List<FileRecord> findByStatusIn(List<FileStatus> statuses);

}
