package com.com4energy.processor.service.dto;

import com.com4energy.processor.model.FailureReason;
import lombok.Builder;

/** Request para registrar un archivo rechazado y su motivo de fallo. */
@Builder
public record FileRejectedRequest(
        String filename,
        String finalPath,
        FailureReason reason,
        String comment
) {}
