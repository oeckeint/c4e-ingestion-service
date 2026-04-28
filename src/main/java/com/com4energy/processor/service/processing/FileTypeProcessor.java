package com.com4energy.processor.service.processing;

import com.com4energy.processor.model.FileRecord;
import com.com4energy.processor.model.FileType;

import java.nio.file.Path;
import java.util.Set;

public interface FileTypeProcessor {

    Set<FileType> supportedTypes();

    FileTypeProcessingResult process(FileRecord fileRecord, Path processingPath);
}

