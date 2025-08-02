package com.com4energy.processor.service.impl;

import java.io.File;
import org.springframework.stereotype.Service;
import com.com4energy.processor.service.FileProcessingService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class FileProcessingServiceImpl implements FileProcessingService {

    @Override
    public void processFile(File file) {
        new Thread(() -> {
            try {
                log.info("Processing file: {}", file.getName());
                Thread.sleep(5000);
                log.info("File processing completed: {}", file.getName());
            } catch (InterruptedException e) {
                log.error("File processing interrupted: {}", file.getName(), e);
            }
        }).start();
    }

}
