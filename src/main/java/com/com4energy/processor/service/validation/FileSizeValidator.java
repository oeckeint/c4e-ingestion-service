package com.com4energy.processor.service.validation;

import com.com4energy.processor.config.properties.FileUploadProperties;
import com.com4energy.processor.model.FailureReason;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Step 3 – rejects files that exceed the configured maximum size. */
@Component
@Order(30)
@RequiredArgsConstructor
public class FileSizeValidator implements FileValidator {

    private final FileUploadProperties props;


    @Override
    public Optional<FailureReason> validate(ValidationContext context) {
        if (context.getFile() == null) {
            return Optional.empty();
        }

        if (context.getFile().getSize() > props.maxSizeBytes()) {
            return Optional.of(FailureReason.FILE_TOO_LARGE);
        }
        return Optional.empty();
    }

    @Override
    public ValidationMode mode() {
        return ValidationMode.COLLECT_ALL;
    }

}
