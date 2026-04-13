package com.com4energy.processor.service.factory;

import com.com4energy.i18n.core.Messages;
import com.com4energy.processor.common.IngestionCommonMessageKey;
import com.com4energy.processor.service.dto.FileContext;
import org.springframework.stereotype.Component;

@Component
public class FileContextMessageFactory {

    public String format(IngestionCommonMessageKey messageKey, FileContext fileContext) {
        return Messages.format(
                messageKey,
                fileContext.validationContext().getOriginalFilename(),
                fileContext.fileStatus()
        );
    }
}
