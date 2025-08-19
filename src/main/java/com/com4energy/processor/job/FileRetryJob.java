package com.com4energy.processor.job;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.com4energy.processor.model.FileRecord;
import com.com4energy.processor.model.FileStatus;
import com.com4energy.processor.repository.FileRecordRepository;
import com.com4energy.processor.service.FileProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileRetryJob {

    private final FileRecordRepository fileRecordRepository;
    private final FileProcessingService fileProcessorService;

    @Value("${file.max-retries:3}")
    private int maxRetries;

    @Scheduled(fixedDelayString = "${file.retry-interval-ms:60000}") // cada 1 min por defecto
    public void retryPendingFiles() {
        List<FileRecord> pendingFiles = fileRecordRepository.findByStatus(FileStatus.PENDING);

        for (FileRecord record : pendingFiles) {
            try {
                log.info("✅ Retrying file processing for: {}", record.getFilename());
                fileProcessorService.processFile(record);
                //record.setStatus(FileStatus.PROCESSED);
                //record.setProcessedAt(LocalDateTime.now());
                log.info("📄 File reprocessed successfully: {}", record.getFilename());
            } catch (Exception e) {
                int retries = record.getRetryCount() == null ? 0 : record.getRetryCount();
                retries++;
                record.setRetryCount(retries);
                record.setLastAttemptAt(LocalDateTime.now());

                if (retries >= maxRetries) {
                    record.setStatus(FileStatus.FAILED);
                    record.setFailedAt(LocalDateTime.now());
                }
            }
            fileRecordRepository.save(record);
        }
    }

}
