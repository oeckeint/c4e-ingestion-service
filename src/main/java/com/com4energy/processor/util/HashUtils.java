package com.com4energy.processor.util;

import com.com4energy.processor.config.AppFeatureProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Slf4j
@Component
@RequiredArgsConstructor
public class HashUtils {

    private final AppFeatureProperties appFeatureProperties;

    public static String calculateHash(File file) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] byteArray = new byte[1024];
            int bytesCount;

            while ((bytesCount = fis.read(byteArray)) != -1) {
                digest.update(byteArray, 0, bytesCount);
            }
        }

        byte[] bytes = digest.digest();

        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }

        return sb.toString();
    }

    public String computeHashIfEnabled(File file) {
        if (!this.appFeatureProperties.isEnabled("compute-hash")) {
            log.info("⚙️ Skipping hash computation for file '{}' due to feature flag.", file.getName());
            return null;
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            return DigestUtils.md5DigestAsHex(fis);
        } catch (IOException e) {
            log.error("❌ Error computing hash for file '{}': {}", file.getAbsolutePath(), e.getMessage());
            return null;
        }
    }

    public String computeHashIfEnabled(MultipartFile file) {
        if (!this.appFeatureProperties.isEnabled("compute-hash")) {
            log.info("⚙️ Skipping hash computation for file '{}' due to feature flag.", file.getOriginalFilename());
            return null;
        }

        try (InputStream is = file.getInputStream()) {
            return DigestUtils.md5DigestAsHex(is);
        } catch (IOException e) {
            log.error("❌ Error computing hash for file '{}': {}", file.getOriginalFilename(), e.getMessage());
            return null;
        }
    }

}
