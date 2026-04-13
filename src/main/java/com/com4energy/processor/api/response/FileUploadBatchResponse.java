package com.com4energy.processor.api.response;

import java.util.List;

public record FileUploadBatchResponse(
        List<FileBatchItemResponse> success,
        List<FileBatchItemResponse> duplicated,
        List<FileBatchItemResponse> invalid
) {}
