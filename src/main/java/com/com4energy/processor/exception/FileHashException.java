package com.com4energy.processor.exception;

import com.com4energy.i18n.core.Messages;
import com.com4energy.processor.common.IngestionExceptionMessageKey;

public class FileHashException extends RuntimeException {

    public FileHashException(String filename, Throwable cause) {
        super(Messages.format(IngestionExceptionMessageKey.FILE_HASH_ERROR, filename), cause);
    }

}
