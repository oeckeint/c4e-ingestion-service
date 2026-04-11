package com.com4energy.processor.exception;

import com.com4energy.i18n.core.Messages;
import com.com4energy.processor.common.IngestionExceptionMessageKey;

public class FileValidatorException extends RuntimeException {

    public FileValidatorException(String filename, String validatorName, Throwable cause) {
        super(Messages.format(IngestionExceptionMessageKey.FILE_VALIDATOR_ERROR, filename, validatorName), cause);
    }

}
