package com.com4energy.processor.util;

import com.com4energy.processor.model.FileRecord;
import com.com4energy.processor.model.FileType;
import com.com4energy.processor.util.medidas.MedidasUtil;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Locale;
import java.util.Set;

public class FileUtils {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "text/csv",
            "text/plain",
            "application/json",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    private FileUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Extracts the lowercase extension from a filename.
     *
     * <p>Edge cases handled:
     * <ul>
     *   <li>{@code .env}      → {@code null}  (hidden Unix file; dot is at index 0)</li>
     *   <li>{@code file.}     → {@code null}  (trailing dot, no extension)</li>
     *   <li>{@code file.tar.gz} → {@code "gz"} (last segment wins)</li>
     *   <li>{@code file..pdf} → {@code "pdf"} (double-dot; safe-filename check rejects this upstream)</li>
     *   <li>{@code file.pdf}  → {@code "pdf"} (normal case)</li>
     * </ul>
     */
    public static String extractExtension(String filename) {
        if (filename == null) {
            return null;
        }

        int lastDot = filename.lastIndexOf('.');

        // lastDot == 0  → hidden file like ".env"  (no real extension)
        // lastDot == -1 → no dot at all
        // lastDot == last char → trailing dot, e.g. "file."
        if (lastDot <= 0 || lastDot == filename.length() - 1) {
            return null;
        }

        return filename.substring(lastDot + 1)
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    /**
     * Returns {@code true} only when {@code filename} is safe to store on the filesystem.
     *
     * <p>Rejected patterns:
     * <ul>
     *   <li>Control characters – null byte {@code \0}, newline {@code \n}, carriage-return {@code \r}, etc.</li>
     *   <li>Path separators – {@code /} and {@code \} (both Unix and Windows traversal vectors)</li>
     *   <li>Double-dot sequences – {@code ..} (directory traversal such as {@code ../../etc/passwd})</li>
     * </ul>
     */
    public static boolean isSafeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return false;
        }

        // Reject any control character (0x00–0x1F) and DEL (0x7F).
        // This covers: \0 (null byte), \n (newline), \r (carriage return), \t (tab), etc.
        for (int i = 0; i < filename.length(); i++) {
            char c = filename.charAt(i);
            if (c < 0x20 || c == 0x7F) {
                return false;
            }
        }

        // Reject path separators – prevents traversal on both Unix and Windows.
        if (filename.contains("/") || filename.contains("\\")) {
            return false;
        }

        // Reject double-dot sequences – prevents directory traversal like "../../etc/passwd".
        if (filename.contains("..")) {
            return false;
        }

        return true;
    }

    public static boolean isValidContentType(MultipartFile file) {
        if (file == null) {
            return false;
        }

        String contentType = file.getContentType();
        return contentType != null && ALLOWED_CONTENT_TYPES.contains(contentType);
    }

    public static void defineAndSetFileTypeToFileRecord(@NotNull FileRecord record, File file){
        record.setType(FileType.UNKNOWN);
        switch (record.getExtension()) {
            case "xml":
                defineXmlType(record, file);
                break;
            case "0":
            case "1":
            case "2":
            case "3":
            case "4":
            case "5":
            case "6":
            case "7":
            case "8":
            case "9":
                MedidasUtil.defineMedidaType(record);
                break;
        }
    }

    private static @NotNull FileRecord defineXmlType (@NotNull FileRecord record, File file){
        record.setType(FileType.FACTURA);
        return record;
    }

}
