package com.com4energy.processor.service;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.com4energy.processor.config.AppFeatureProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.com4energy.processor.config.properties.FileScannerProperties;
import lombok.RequiredArgsConstructor;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileScannerService {

    private final AppFeatureProperties appFeatureProperties;
    private final FileRecordService fileRecordService;
    private final FileScannerProperties fileScannerProperties;

    public void scanAndRegisterFiles() {
        for (String pathStr : fileScannerProperties.getPaths()) {
            Path path = Paths.get(pathStr);
            if (!Files.exists(path) || !Files.isDirectory(path)) {
                continue;
            }

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
                for (Path file : stream) {
                    if (Files.isRegularFile(file)) {
                        String filename = file.getFileName().toString();
                        if (!appFeatureProperties.isEnabled("persist-data")) {
                            log.info("Persist records disabled by feature flag. Ignoring file: {}", filename);
                            continue;
                        }
                        log.error("needs a new implementation for registering files as pending from job");
                        //fileRecordService.registerFileAsPendingIntoDatababase(filename, file.toAbsolutePath().toString(), FileOrigin.JOB);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Error scanning directory: " + pathStr, e);
            }
        }
    }

}
