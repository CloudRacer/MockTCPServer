package io.cloudracer.properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import io.cloudracer.AbstractTestTools;
import io.cloudracer.TestConstants;

/**
 * Configuration initialisation (i.e. write a default configuration file to disk if one does not already exist) and modification.
 *
 * @author John McDonnell
 *
 */
public class TestServerConfigurationSettingsEnabledST extends AbstractTestTools {

    private static Logger logger = LogManager.getLogger();

    /**
     * BeforeClass
     *
     * @throws IOException
     */
    @BeforeClass
    public static void setupClass() throws IOException {
        resetConfiguration();

        System.getProperties().put(TestConstants.CONFIGURATION_INITIALISATION_ENABLED_PROPERTY_NAME, TestConstants.TRUE);

        logger.debug(String.format("Properties: %s.", StringUtils.join(System.getProperties())));
    }

    /**
     * AfterClass
     *
     * @throws IOException
     */
    @Override
    @Before
    public void setUp() throws IOException {
        deleteConfigurationFolder();
    }

    @Override
    @After
    public void cleanUp() throws IOException {
        deleteConfigurationFolder();
    }

    /**
     * Initialise the configuration (i.e. write a default configuration to disk, if one does not already exist) and then modify it.
     *
     * @throws ConfigurationException
     */
    @Test
    public void configurationInitialisation() throws ConfigurationException {
        assertEquals(TestConstants.TRUE, System.getProperty(TestConstants.CONFIGURATION_INITIALISATION_ENABLED_PROPERTY_NAME));
        assertTrue(this.getConfigurationSettings().getFileName().getFile().endsWith(TestConstants.MOCKTCPSERVER_XML_FULL_PATH_SUFFIX));
        assertTrue(new File(this.getConfigurationSettings().getFileName().getFile()).exists());
        assertEquals(TestConstants.PORT_SET, this.getConfigurationSettings().getPorts());
    }
}