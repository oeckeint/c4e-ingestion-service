package com.com4energy.processor.service.validation;

import com.com4energy.processor.model.FailureReason;
import com.com4energy.processor.service.FileRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Step 7 – rejects files whose name already exists in the database.
 *
 * <p>This is intentionally the <em>last</em> step: it hits the database, so
 * it should only run once all cheap, local validations have passed.
 *
 * <p>{@link FilenameValidator} runs before this step, so the originalFilename is
 * guaranteed to be non-null and safe at this point.
 */
@Component
@Order(210)
@RequiredArgsConstructor
public class DuplicatedOriginalFilenameValidator implements FileValidator {

    private final FileRecordService fileRecordService;

    @Override
    public Optional<FailureReason> validate(ValidationContext context) {
        String originalFilename = context.getOriginalFilename();

        if (originalFilename != null && fileRecordService.existsByFilename(originalFilename)) {
            return Optional.of(FailureReason.DUPLICATED_ORIGINAL_FILENAME);
        }
        return Optional.empty();
    }

}
