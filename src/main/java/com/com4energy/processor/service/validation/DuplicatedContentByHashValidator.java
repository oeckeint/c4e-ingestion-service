package com.com4energy.processor.service.validation;

import com.com4energy.processor.model.FailureReason;
import com.com4energy.processor.service.FileRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Step 300 – rejects files whose content already exists in the database (hash-based dedup).
 *
 * <p>Unlike {@link DuplicateFilenameValidator} (which only checks the originalFilename), this validator
 * computes a SHA-256 hash of the file bytes and queries the database to detect identical content
 * uploaded under a different name.
 *
 * <p>The hash is computed lazily via {@link ValidationContext#getOrComputeHash()} and cached
 * inside the context, so if multiple validators need the hash in the same request it is only
 * computed once.
 *
 * <p>This validator intentionally runs last ({@code @Order(300)}) because hashing is I/O-bound
 * and the database query is an extra round-trip; both operations should only execute once all
 * cheap local validations have already passed.
 */
@Component
@Order(300)
@RequiredArgsConstructor
public class DuplicatedContentByHashValidator implements FileValidator {

    private final FileRecordService fileRecordService;

    @Override
    public Optional<FailureReason> validate(ValidationContext context) {
        if (context.getFile() == null) {
            return Optional.empty();
        }

        if (fileRecordService.existsByHash(context.getOrComputeHash())) {
            return Optional.of(FailureReason.DUPLICATED_CONTENT);
        }

        return Optional.empty();
    }

}
