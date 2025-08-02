package com.com4energy.processor.repository;

import com.com4energy.processor.model.FileRecord;
import com.com4energy.processor.model.FileStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileRecordRepository extends JpaRepository<FileRecord, Long> {

    Optional<FileRecord> findByFilenameAndPath(String filename, String path);

    List<FileRecord> findByStatus(FileStatus status);

}
