package com.com4energy.processor.util;

import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public class FileUtils {

    private static final String GENERIC_BINARY_CONTENT_TYPE = "application/octet-stream";

    private FileUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Extracts the lowercase extension from a originalFilename.
     *
     * <p>Edge cases handled:
     * <ul>
     *   <li>{@code .env}      → {@code null}  (hidden Unix file; dot is at index 0)</li>
     *   <li>{@code file.}     → {@code null}  (trailing dot, no extension)</li>
     *   <li>{@code file.tar.gz} → {@code "gz"} (last segment wins)</li>
     *   <li>{@code file..pdf} → {@code "pdf"} (double-dot; safe-originalFilename check rejects this upstream)</li>
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
     * Returns {@code true} only when {@code originalFilename} is safe to store on the filesystem.
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

    public static boolean isEmptyBatch(MultipartFile[] files) {
        return files == null || files.length == 0;
    }

    public static boolean isValidContentType(MultipartFile file, Set<String> allowedContentTypes) {
        if (file == null) {
            return false;
        }

        Set<String> normalizedAllowedContentTypes = normalizeAllowedContentTypes(allowedContentTypes);
        String normalizedContentType = normalizeContentType(file.getContentType());
        if (normalizedContentType != null && isAllowedXmlContentType(normalizedContentType, normalizedAllowedContentTypes)) {
            return true;
        }

        if (normalizedContentType != null && !GENERIC_BINARY_CONTENT_TYPE.equals(normalizedContentType)) {
            return false;
        }

        String extension = extractExtension(file.getOriginalFilename());
        return "xml".equals(extension) && hasExpectedXmlPayload(file);
    }

    public static String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return null;
        }

        String normalized = contentType.trim().toLowerCase(Locale.ROOT);
        int parametersStart = normalized.indexOf(';');
        if (parametersStart >= 0) {
            normalized = normalized.substring(0, parametersStart).trim();
        }
        return normalized;
    }

    private static boolean isAllowedXmlContentType(String normalizedContentType, Set<String> normalizedAllowedContentTypes) {
        return normalizedAllowedContentTypes.contains(normalizedContentType)
                || normalizedContentType.endsWith("+xml");
    }

    private static Set<String> normalizeAllowedContentTypes(Set<String> allowedContentTypes) {
        if (allowedContentTypes == null || allowedContentTypes.isEmpty()) {
            return Collections.emptySet();
        }

        return allowedContentTypes.stream()
                .filter(Objects::nonNull)
                .map(contentType -> contentType.trim().toLowerCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private static boolean hasExpectedXmlPayload(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            byte[] firstBytes = inputStream.readNBytes(4096);
            if (firstBytes.length == 0) {
                return false;
            }

            String probe = new String(firstBytes, StandardCharsets.UTF_8);
            if (!probe.isEmpty() && probe.charAt(0) == '\uFEFF') {
                probe = probe.substring(1);
            }
            probe = probe.stripLeading();

            return probe.startsWith("<?xml") && probe.contains("<MensajeFacturacion");
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Builds a hardened DocumentBuilderFactory for XML parsing.
     *
     * <p>XXE and external entity resolution are disabled to reduce attack surface.
     */
    public static DocumentBuilderFactory newSecureDocumentBuilderFactory() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);

            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        } catch (ParserConfigurationException | IllegalArgumentException e) {
            throw new IllegalStateException("Unable to configure secure DocumentBuilderFactory", e);
        }

        return factory;
    }

    /**
     * Builds a hardened SchemaFactory for XSD validation.
     */
    public static SchemaFactory newSecureSchemaFactoryForXsd() {
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try {
            schemaFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        } catch (SAXNotRecognizedException | SAXNotSupportedException | IllegalArgumentException e) {
            throw new IllegalStateException("Unable to configure secure SchemaFactory", e);
        }

        return schemaFactory;
    }

    /**
     * Parses XML with hardened settings and validates it against the provided XSD.
     *
     * @throws SAXException when XML is malformed or does not comply with the schema.
     * @throws IOException  when stream reading fails.
     */
    public static void validateXmlAgainstXsd(InputStream xmlInputStream, Source xsdSource)
            throws SAXException, IOException {
        Objects.requireNonNull(xmlInputStream, "xmlInputStream cannot be null");
        Objects.requireNonNull(xsdSource, "xsdSource cannot be null");

        DocumentBuilder builder;
        try {
            builder = newSecureDocumentBuilderFactory().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("Unable to create secure DocumentBuilder", e);
        }

        Document xmlDocument = builder.parse(xmlInputStream);

        SchemaFactory schemaFactory = newSecureSchemaFactoryForXsd();
        Schema schema = schemaFactory.newSchema(xsdSource);
        Validator validator = schema.newValidator();
        validator.validate(new DOMSource(xmlDocument));
    }

}
