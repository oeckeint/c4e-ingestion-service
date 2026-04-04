package com.com4energy.processor.service.dto;

import com.com4energy.processor.api.response.FileMetadata;
import lombok.Builder;

import java.util.List;

@Builder
public record UploadBatchResult(
        int duplicates,
        int errors,
        int processed,
        List<FileMetadata> uploadedData,
        int uploadedCount
) {}
