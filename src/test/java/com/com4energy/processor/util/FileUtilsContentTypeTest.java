package com.com4energy.processor.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileUtilsContentTypeTest {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "text/xml",
            "application/xml",
            "application/x-xml"
    );

    @ParameterizedTest
    @ValueSource(strings = {
            "text/xml",
            "text/xml; charset=UTF-8",
            "application/xml",
            "application/xml; charset=UTF-8",
            "application/rss+xml",
            "application/soap+xml",
            "application/vnd.record+xml"
    })
    void isValidContentType_acceptsExpectedXmlTypes(String contentType) {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "valid.xml",
                contentType,
                "<root/>".getBytes()
        );

        assertTrue(FileUtils.isValidContentType(file, ALLOWED_CONTENT_TYPES));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "application/json",
            "text/plain",
            "application/pdf",
            "image/png",
            "application/octet-stream"
    })
    void isValidContentType_rejectsUnexpectedTypes(String contentType) {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "invalid.xml",
                contentType,
                "<root/>".getBytes()
        );

        assertFalse(FileUtils.isValidContentType(file, ALLOWED_CONTENT_TYPES));
    }

    @ParameterizedTest
    @ValueSource(strings = {"application/octet-stream", "application/octet-stream; charset=UTF-8"})
    void isValidContentType_acceptsGenericBinaryOnlyWhenExpectedXmlPayload(String contentType) {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><MensajeFacturacion/>";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "valid.xml",
                contentType,
                xml.getBytes()
        );

        assertTrue(FileUtils.isValidContentType(file, ALLOWED_CONTENT_TYPES));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void isValidContentType_acceptsMissingTypeOnlyWhenExpectedXmlPayload(String contentType) {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><MensajeFacturacion/>";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "valid.xml",
                contentType,
                xml.getBytes()
        );

        assertTrue(FileUtils.isValidContentType(file, ALLOWED_CONTENT_TYPES));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void isValidContentType_rejectsMissingOrBlankType(String contentType) {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "invalid.xml",
                contentType,
                "<root/>".getBytes()
        );

        assertFalse(FileUtils.isValidContentType(file, ALLOWED_CONTENT_TYPES));
    }
}
