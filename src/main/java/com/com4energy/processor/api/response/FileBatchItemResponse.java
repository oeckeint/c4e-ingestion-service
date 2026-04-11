package com.com4energy.processor.api.response;

import com.com4energy.processor.model.FileStatus;

public record FileBatchItemResponse(
        String originalFilename,
        String filename,
        FileStatus status
) {
}

