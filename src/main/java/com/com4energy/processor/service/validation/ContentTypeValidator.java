package com.com4energy.processor.service.validation;

import com.com4energy.processor.config.properties.FileUploadProperties;
import com.com4energy.processor.model.FailureReason;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static com.com4energy.processor.util.FileUtils.isValidContentType;

/** Step 6 – rejects files whose MIME content-type is not in the allow-list. */
@Component
@Order(130)
@RequiredArgsConstructor
public class ContentTypeValidator implements FileValidator {

    private final FileUploadProperties fileUploadProperties;

    @Override
    public Optional<FailureReason> validate(ValidationContext context) {
        if (context.getFile() == null || context.getOriginalFilename() == null) {
            return Optional.empty();
        }

        if (!isValidContentType(context.getFile(), fileUploadProperties.normalizedAllowedContentTypes())) {
            return Optional.of(FailureReason.INVALID_CONTENT_TYPE);
        }
        return Optional.empty();
    }

    @Override
    public ValidationMode mode() {
        return ValidationMode.COLLECT_ALL;
    }

}
