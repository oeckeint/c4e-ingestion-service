package com.com4energy.processor.service.validation;

import com.com4energy.i18n.core.util.StringRules;
import com.com4energy.processor.exception.FileHashException;
import com.com4energy.processor.util.HashUtils;
import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.web.multipart.MultipartFile;

import static com.com4energy.processor.util.FileUtils.extractExtension;

@Getter
public class ValidationContext {

    private final MultipartFile file;
    private final String originalFilename;
    private final String extension;

    @Getter(AccessLevel.NONE)
    private String computedHash; // lazy cache

    private ValidationContext(MultipartFile file, String originalFilename, String extension) {
        this.file = file;
        this.originalFilename = originalFilename;
        this.extension = extension;
    }

    public static ValidationContext from(MultipartFile file) {
        String originalFilename = file == null ? null : StringRules.trimOrNull(file.getOriginalFilename());
        String extension = extractExtension(originalFilename);
        return new ValidationContext(file, originalFilename, extension);
    }

    public String getOrComputeHash() {
        if (computedHash == null) {
            try {
                if (file == null) {
                    throw new IllegalStateException("Cannot compute hash: file is null");
                }
                computedHash = HashUtils.sha256(file);
            } catch (RuntimeException ex) {
                throw new FileHashException(this.originalFilename, ex);
            }
        }
        return computedHash;
    }

}
