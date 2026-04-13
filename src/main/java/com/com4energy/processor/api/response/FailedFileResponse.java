package com.com4energy.processor.api.response;

import java.util.List;

public record FailedFileResponse(
        String filename,
        String hash,
        List<FailureReasonResponse> reasons) {}


