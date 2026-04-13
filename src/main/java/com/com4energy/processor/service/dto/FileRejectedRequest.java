package com.com4energy.processor.service.dto;

import com.com4energy.processor.model.FailureReason;
import lombok.Builder;

import java.util.List;

/** Request para registrar un archivo rechazado y su motivo de fallo. */
@Builder
public record FileRejectedRequest(
        String finalFilename,
        String originalFilename,
        String finalPath,
        FailureReason reason,
        List<FailureReason> reasons,
        String comment,
        String hash
) {}
