package com.com4energy.processor.service.mapper;

import com.com4energy.i18n.core.Messages;
import com.com4energy.processor.common.IngestionCommonMessageKey;
import com.com4energy.processor.model.FailureReason;
import com.com4energy.processor.service.dto.FileContext;
import com.com4energy.processor.service.dto.FileRejectedRequest;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class FileRejectedRequestMapper {

    public FileRejectedRequest from(FileContext fileContext) {
        return FileRejectedRequest.builder()
                .originalFilename(fileContext.validationContext().getOriginalFilename())
                .finalPath(fileContext.findStoredFilePath().orElse(null))
                .hash(fileContext.validationContext().getOrComputeHash())
                .reason(fileContext.getPrimaryFailureReason())
                .reasons(fileContext.failureReasons())
                .comment(buildRejectionComment(fileContext))
                .build();
    }

    private String buildRejectionComment(FileContext fileContext) {
        if (fileContext.failureReasons().size() > 1) {
            String all = fileContext.failureReasons().stream()
                    .map(FailureReason::name)
                    .collect(Collectors.joining(", "));
            return Messages.format(
                    IngestionCommonMessageKey.FILE_REJECTED_MULTIPLE_REASONS_COMMENT,
                    fileContext.getPrimaryFailureReason().getDescription(),
                    all
            );
        }
        return fileContext.getPrimaryFailureReason().getDescription();
    }

}

