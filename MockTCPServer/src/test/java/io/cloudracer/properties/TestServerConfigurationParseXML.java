package io.cloudracer.properties;

import java.io.IOException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Parse the XML configuration document, used for testing, to endure that it is properly formated.
 *
 * @author John McDonnell
 */
public class TestServerConfigurationParseXML {

    /**
     * Parse the XML configuration document that is used for testing.
     *
     * @throws Exception
     *
     */
    @Test
    public void parseXML() throws Exception { // NOSONAR
        final String xml = "/mocktcpserver.xml";
        final String schemaName = "/mocktcpserver.xsd";

        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setNamespaceAware(true);

        DocumentBuilder parser;
        try {
            parser = builderFactory.newDocumentBuilder();

            Document document = parser.parse(ClassLoader.class.getResourceAsStream(xml));

            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

            Source schemaFile = new StreamSource(ClassLoader.class.getResourceAsStream(schemaName));
            Schema schema = factory.newSchema(schemaFile);

            Validator validator = schema.newValidator();
            validator.validate(new DOMSource(document));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw e; // NOSONAR
        }
    }
}
