package com.com4energy.processor.service.measure;

import com.com4energy.processor.model.FileType;

import java.util.List;

public record MeasureFilenameMetadata(
        String originalFilename,
        MeasureFileKind kind,
        List<String> tokens,
        int extensionDigit
) {

    public MeasureFilenameMetadata {
        tokens = List.copyOf(tokens);
    }

    public FileType fileType() {
        return kind.toFileType();
    }
}

