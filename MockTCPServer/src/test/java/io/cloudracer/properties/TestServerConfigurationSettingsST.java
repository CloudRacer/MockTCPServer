package io.cloudracer.properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.configuration2.ex.ConfigurationException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.SAXException;

import io.cloudracer.AbstractTestTools;
import io.cloudracer.TestConstants;

/**
 * Test that the property file can be located, and read/written.
 *
 * @author John McDonnell
 *
 */
public class TestServerConfigurationSettingsST extends AbstractTestTools {

    /**
     * BeforeClass
     *
     * @throws IOException
     */
    @BeforeClass
    public static void setupClass() throws IOException {
        backupConfigurationFile();

        initialiseSystemProperties();
    }

    /**
     * AfterClass
     *
     * @throws IOException
     */
    @AfterClass
    public static void cleanupClass() throws IOException {
        resetConfiguration();
    }

    /**
     * Before
     *
     * @throws IOException
     */
    @Before
    public void setup() throws IOException {
        resetConfiguration();
    }

    /**
     * After
     *
     * @throws IOException
     */
    @Override
    @After
    public void cleanUp() throws IOException {
        resetConfiguration();
    }

    /**
     * By default, the resource file is used as the configuration file.
     */
    @Test
    public void locateConfigurationFile() {
        assertTrue(this.getConfigurationSettings().getFileName().getFile().endsWith(TestConstants.MOCKTCPSERVER_XML_FULL_RESOURCE_PATH_SUFFIX));
    }

    /**
     * Get the servers port from the configuration file.
     *
     * @throws ConfigurationException
     */
    @Test
    public void retrieveProperties() throws ConfigurationException {
        assertEquals(TestConstants.MOCK_SERVER_PORT, this.getConfigurationSettings().getPort());
    }

    /**
     * Update the servers port in the configuration file.
     *
     * @throws ConfigurationException
     */
    @Test
    public void setPropertyValue() throws ConfigurationException {
        assertEquals(TestConstants.MOCK_SERVER_PORT, this.getConfigurationSettings().getPort());

        this.getConfigurationSettings().setPort(TestConstants.MOCK_SERVER_NEW_PORT);
        assertEquals(TestConstants.MOCK_SERVER_NEW_PORT, this.getConfigurationSettings().getPort());

        this.getConfigurationSettings().setPort(TestConstants.MOCK_SERVER_PORT);
        assertEquals(TestConstants.MOCK_SERVER_PORT, this.getConfigurationSettings().getPort());
    }

    /**
     * Get the server responses from the configuration file.
     *
     * @throws ConfigurationException
     * @throws XPathExpressionException
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    @Test
    public void getResponses() throws ConfigurationException, XPathExpressionException, ParserConfigurationException, SAXException, IOException {
        assertEquals(TestConstants.MOCK_SERVER_PORT, this.getConfigurationSettings().getPort());

        assertEquals(TestConstants.EXPECTED_INCOMING_ALL_MESSAGE_RESPONSES_RESULT, this.getConfigurationSettings().getResponses(TestConstants.MOCK_SERVER_PORT).toString());
    }
}