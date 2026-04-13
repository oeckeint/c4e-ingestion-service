package com.com4energy.processor.api.response;

import com.com4energy.processor.model.FileStatus;
import lombok.Builder;

@Builder
public record FileMetadata(
        String originalFilename,
        FileStatus status,
        String hash) {}
