package io.cloudracer.properties;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestServerConfigurationSettingsST {

    private ConfigurationSettings server;

    private static final String MOCKTCPSERVER_XML_FULL_PATH = "file:/C:/src/MockTCPServer/MockTCPServer/MockTCPServer/target/classes/mocktcpserver.xml";
    private static final int MOCKTCPSERVER_XML_PORT = 6789;
    private static final int MOCKTCPSERVER_XML_NEW_PORT = 1111;

    @Before
    public void setup() throws IOException {
        // Start without a file.
        final File propertiesDirectory = new File("configuration");

        FileUtils.deleteDirectory(propertiesDirectory);

        this.server = new ConfigurationSettings();
    }

    @After
    public void cleanUp() throws IOException, ConfigurationException {
        // Reset the property values.
        this.server.setPort(MOCKTCPSERVER_XML_PORT);
    }

    @Test
    public void locateConfigurationFile() throws MalformedURLException {
        assertEquals(new URL(MOCKTCPSERVER_XML_FULL_PATH), this.server.getFileName());
    }

    @Test
    public void retrieveProperties() throws ConfigurationException {
        assertEquals(MOCKTCPSERVER_XML_PORT, this.server.getPort());
    }

    @Test
    public void setPropertyValue() throws ConfigurationException {
        assertEquals(MOCKTCPSERVER_XML_PORT, this.server.getPort());

        this.server.setPort(MOCKTCPSERVER_XML_NEW_PORT);
        assertEquals(MOCKTCPSERVER_XML_NEW_PORT, this.server.getPort());

        this.server.setPort(MOCKTCPSERVER_XML_PORT);
        assertEquals(MOCKTCPSERVER_XML_PORT, this.server.getPort());
    }
}