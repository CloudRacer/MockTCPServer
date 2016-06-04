package io.cloudracer.properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.MalformedURLException;

import org.apache.commons.configuration2.ex.ConfigurationException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import io.cloudracer.AbstractTestTools;
import io.cloudracer.TestConstants;

public class TestServerConfigurationSettingsST extends AbstractTestTools {

    @BeforeClass
    public static void setupClass() throws IOException {
        backupConfigurationFile();

        initialiseSystemProperties();
    }

    @AfterClass
    public static void cleanupClass() throws IOException {
        resetConfiguration();
    }

    @Before
    public void setup() throws IOException {
        resetConfiguration();
    }

    @Override
    @After
    public void cleanUp() throws IOException {
        resetConfiguration();
    }

    /**
     * By default, the resource file is used as the configuration file.
     *
     * @throws MalformedURLException
     */
    @Test
    public void locateConfigurationFile() {
        assertTrue(this.getConfigurationSettings().getFileName().getFile().endsWith(TestConstants.MOCKTCPSERVER_XML_FULL_RESOURCE_PATH_SUFFIX));
    }

    @Test
    public void retrieveProperties() throws ConfigurationException {
        assertEquals(TestConstants.MOCK_SERVER_PORT, this.getConfigurationSettings().getPort());
    }

    @Test
    public void setPropertyValue() throws ConfigurationException {
        assertEquals(TestConstants.MOCK_SERVER_PORT, this.getConfigurationSettings().getPort());

        this.getConfigurationSettings().setPort(TestConstants.MOCK_SERVER_NEW_PORT);
        assertEquals(TestConstants.MOCK_SERVER_NEW_PORT, this.getConfigurationSettings().getPort());

        this.getConfigurationSettings().setPort(TestConstants.MOCK_SERVER_PORT);
        assertEquals(TestConstants.MOCK_SERVER_PORT, this.getConfigurationSettings().getPort());
    }
}