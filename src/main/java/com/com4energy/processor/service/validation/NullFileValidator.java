package com.com4energy.processor.service.validation;

import com.com4energy.processor.model.FailureReason;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Step 1 – rejects a {@code null} reference before any other check. */
@Component
@Order(10)
public class NullFileValidator implements FileValidator {

    @Override
    public Optional<FailureReason> validate(ValidationContext context) {
        if (context.getFile() == null) {
            return Optional.of(FailureReason.NULL_FILE);
        }
        return Optional.empty();
    }

    @Override
    public ValidationMode mode() {
        return ValidationMode.COLLECT_ALL;
    }

}

