package com.com4energy.processor.util;

import org.junit.jupiter.api.Test;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileUtilsXmlSecurityTest {

    @Test
    void secureDocumentBuilderFactoryRejectsDoctype() throws Exception {
        DocumentBuilderFactory factory = FileUtils.newSecureDocumentBuilderFactory();
        DocumentBuilder builder = factory.newDocumentBuilder();

        String maliciousXml = "<?xml version=\"1.0\"?>"
                + "<!DOCTYPE foo [<!ELEMENT foo ANY >"
                + "<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]>"
                + "<foo>&xxe;</foo>";

        assertThrows(Exception.class,
                () -> builder.parse(new InputSource(new StringReader(maliciousXml))));
    }

    @Test
    void secureSchemaFactoryCreatesSchemaFromInlineXsd() throws Exception {
        SchemaFactory schemaFactory = FileUtils.newSecureSchemaFactoryForXsd();

        String xsd = "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">"
                + "<xs:element name=\"foo\" type=\"xs:string\"/>"
                + "</xs:schema>";

        Schema schema = schemaFactory.newSchema(new StreamSource(new StringReader(xsd)));
        assertNotNull(schema);
    }

    @Test
    void validateXmlAgainstXsdAcceptsValidXml() {
        String xsd = "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">"
                + "<xs:element name=\"foo\" type=\"xs:string\"/>"
                + "</xs:schema>";
        Source xsdSource = new StreamSource(new StringReader(xsd));

        String validXml = "<foo>ok</foo>";

        assertDoesNotThrow(() -> FileUtils.validateXmlAgainstXsd(
                new ByteArrayInputStream(validXml.getBytes(StandardCharsets.UTF_8)),
                xsdSource
        ));
    }

    @Test
    void validateXmlAgainstXsdRejectsInvalidXml() {
        String xsd = "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">"
                + "<xs:element name=\"foo\" type=\"xs:string\"/>"
                + "</xs:schema>";
        Source xsdSource = new StreamSource(new StringReader(xsd));

        String invalidXml = "<bar>nope</bar>";

        assertThrows(SAXException.class, () -> FileUtils.validateXmlAgainstXsd(
                new ByteArrayInputStream(invalidXml.getBytes(StandardCharsets.UTF_8)),
                xsdSource
        ));
    }
}

