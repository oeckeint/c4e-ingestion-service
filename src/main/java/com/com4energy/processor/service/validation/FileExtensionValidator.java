package com.com4energy.processor.service.validation;

import com.com4energy.processor.config.properties.FileUploadProperties;
import com.com4energy.processor.model.FailureReason;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Step 5 – rejects files whose extension is not in the allow-list.
 *
 * <p>The allowed set comes from
 * {@link FileUploadProperties#normalizedAllowedExtensions()}.
 *
 * <p>{@link FilenameValidator} runs before this step, guaranteeing that the
 * originalFilename is non-null and free of traversal characters.
 */
@Component
@Order(120)
@RequiredArgsConstructor
public class FileExtensionValidator implements FileValidator {

    private final FileUploadProperties props;

    @Override
    public Optional<FailureReason> validate(ValidationContext context) {
        if (context.getOriginalFilename() == null) {
            return Optional.empty();
        }

        String ext = context.getExtension();

        if (ext == null || !props.normalizedAllowedExtensions().contains(ext)) {
            return Optional.of(FailureReason.INVALID_EXTENSION);
        }
        return Optional.empty();
    }

    @Override
    public ValidationMode mode() {
        return ValidationMode.COLLECT_ALL;
    }

}
