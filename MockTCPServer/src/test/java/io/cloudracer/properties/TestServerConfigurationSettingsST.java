package io.cloudracer.properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.MalformedURLException;

import org.apache.commons.configuration2.ex.ConfigurationException;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import io.cloudracer.AbstractTestTools;

public class TestServerConfigurationSettingsST extends AbstractTestTools {

    @BeforeClass
    public static void setupClass() throws IOException {
        recreateTargetConfigurationFile();

        initialiseSystemProperties();
    }

    @Before
    public void setup() throws IOException {
        deleteConfigurationFolder();
    }

    @Override
    @After
    public void cleanUp() throws IOException {
        deleteConfigurationFolder();
    }

    /**
     * By default, the resource file is used as the configuration file.
     *
     * @throws MalformedURLException
     */
    @Test
    public void locateConfigurationFile() {
        assertTrue(this.getConfigurationSettings().getFileName().getFile().endsWith(MOCKTCPSERVER_XML_FULL_RESOURCE_PATH_SUFFIX));
    }

    @Test
    public void retrieveProperties() throws ConfigurationException {
        assertEquals(MOCK_SERVER_PORT, this.getConfigurationSettings().getPort());
    }

    @Test
    public void setPropertyValue() throws ConfigurationException {
        assertEquals(MOCK_SERVER_PORT, this.getConfigurationSettings().getPort());

        this.getConfigurationSettings().setPort(MOCK_SERVER_NEW_PORT);
        assertEquals(MOCK_SERVER_NEW_PORT, this.getConfigurationSettings().getPort());

        this.getConfigurationSettings().setPort(MOCK_SERVER_PORT);
        assertEquals(MOCK_SERVER_PORT, this.getConfigurationSettings().getPort());
    }
}