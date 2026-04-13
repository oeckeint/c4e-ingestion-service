package com.com4energy.processor.service.validation;

import com.com4energy.processor.model.FailureReason;

import java.util.Optional;

/**
 * Chain-of-Responsibility contract for file upload validations.
 *
 * <p>Each implementation checks a single concern and returns the first
 * {@link FailureReason} it finds, or {@link Optional#empty()} if the file
 * passes that step.
 */
public interface FileValidator {

    Optional<FailureReason> validate(ValidationContext context);

    default ValidationMode mode() {
        return ValidationMode.FAIL_FAST;
    }

    default boolean isFailFast() {
        return mode() == ValidationMode.FAIL_FAST;
    }

}
