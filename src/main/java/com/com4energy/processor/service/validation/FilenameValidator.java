package com.com4energy.processor.service.validation;

import com.com4energy.processor.model.FailureReason;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static com.com4energy.processor.util.FileUtils.isSafeFilename;

/**
 * Step 4 – validates the originalFilename itself.
 *
 * <p>Two checks are combined here because both map to the same
 * {@link FailureReason#INVALID_FILENAME} and are semantically inseparable:
 * <ol>
 *   <li>The originalFilename must not be blank after trimming.</li>
 *   <li>It must pass the filesystem-safety rules (no control chars, no path
 *       separators, no {@code ..} traversal sequences).</li>
 * </ol>
 *
 * <p>Downstream validators (extension, duplicate) can therefore assume the
 * originalFilename is non-null and safe.
 */
@Component
@Order(110)
public class FilenameValidator implements FileValidator {

    @Override
    public Optional<FailureReason> validate(ValidationContext context) {
        if (context.getFile() == null) {
            return Optional.empty();
        }

        String filename = context.getOriginalFilename();

        if (filename == null) {
            return Optional.of(FailureReason.INVALID_FILENAME);
        }

        if (!isSafeFilename(filename)) {
            return Optional.of(FailureReason.INVALID_FILENAME);
        }

        return Optional.empty();
    }

    @Override
    public ValidationMode mode() {
        return ValidationMode.COLLECT_ALL;
    }

}
