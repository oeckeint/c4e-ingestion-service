package com.com4energy.processor.api.response;

import com.com4energy.processor.model.FailureReason;

public record FailureReasonResponse(
        String code,
        String message) {

    public static FailureReasonResponse from(FailureReason reason) {
        return new FailureReasonResponse(reason.name(), reason.getDescription());
    }

}
