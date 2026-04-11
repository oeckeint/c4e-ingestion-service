package com.com4energy.processor.service.validation;

import com.com4energy.processor.model.FailureReason;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Step 2 – rejects files with zero bytes. */
@Component
@Order(20)
public class EmptyFileValidator implements FileValidator {

    @Override
    public Optional<FailureReason> validate(ValidationContext context) {
        if (context.getFile() == null) {
            return Optional.empty();
        }

        if (context.getFile().isEmpty()) {
            return Optional.of(FailureReason.FILE_IS_EMPTY);
        }
        return Optional.empty();
    }

    @Override
    public ValidationMode mode() {
        return ValidationMode.COLLECT_ALL;
    }
}

