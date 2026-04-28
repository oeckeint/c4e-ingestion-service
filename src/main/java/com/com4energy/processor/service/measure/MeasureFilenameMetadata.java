package com.com4energy.processor.service.measure;

import com.com4energy.processor.model.FileType;

public record MeasureFilenameMetadata(
        String originalFilename,
        FileType kind
) {

    public FileType fileType() {
        return kind;
    }
}
