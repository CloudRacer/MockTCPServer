package io.cloudracer;

import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;

import io.cloudracer.mocktcpserver.MockTCPServer;
import io.cloudracer.mocktcpserver.tcpclient.TCPClient;
import io.cloudracer.properties.ConfigurationSettings;

/**
 * Tools <b>exclusively</b> for use by test routines.
 *
 * @author John McDonnell
 */
public abstract class AbstractTestTools extends TestConstants {

    private final static Logger logger = LogManager.getLogger();

    private final static File propertiesSourceFile = new File(MOCKTCPSERVER_XML_RESOURCE_TARGET_FILE_NAME);
    private final static File propertiesBackupFile = new File(String.format("%s%s%s-backup.%s", propertiesSourceFile.getParent(), File.separatorChar, FilenameUtils.getBaseName(propertiesSourceFile.getName()), FilenameUtils.getExtension(propertiesSourceFile.getName())));

    private TCPClient client;
    private MockTCPServer server;
    private ConfigurationSettings configurationSettings;

    protected static void initialiseSystemProperties() throws IOException {
        System.getProperties().remove(CONFIGURATION_INITIALISATION_ENABLED_PROPERTY_NAME);

        logger.debug(String.format("Properties: %s.", StringUtils.join(System.getProperties())));
    }

    protected void setUp() throws IOException {
        this.resetLogMonitor();

        this.getServer();
        this.getClient();
    }

    protected void cleanUp() throws IOException {
        this.close();
    }

    /**
     * Asserts that the "TEST" {@link Appender log4j Appender} did not log any messages. Adjust the log4j.xml Appenders to match on only the messages that should cause this method to raise {@link AssertionError}.
     */
    protected final void checkLogMonitorForUnexpectedMessages() {
        try {
            final int delayDuration = 1;
            // Pause to allow messages to be flushed to the disk (and, hence, through the appenders).
            TimeUnit.SECONDS.sleep(delayDuration);
        } catch (final InterruptedException e) {
            // Do nothing
        }
        assertNull(String.format("An unexpected message was logged to the file \"%s\".", LogMonitor.getFileName()), LogMonitor.getLastEventLogged());
    }

    protected void resetLogMonitor() {
        LogMonitor.setLastEventLogged(null);
    }

    protected TCPClient getClient() throws IOException {
        if (this.client == null) {
            this.client = new TCPClient();
        }

        return this.client;
    }

    protected void setClient(TCPClient client) throws IOException {
        if (client == null && this.client != null) {
            this.client.close();
        }

        this.client = client;
    }

    protected MockTCPServer getServer() {
        if (this.server == null) {
            this.server = new MockTCPServer();
        }

        return this.server;
    }

    public ConfigurationSettings getConfigurationSettings() {
        if (this.configurationSettings == null) {
            this.configurationSettings = new ConfigurationSettings();
        }

        return this.configurationSettings;
    }

    protected static void deleteConfigurationFolder() throws IOException {
        final File propertiesDirectory = new File("configuration");

        FileUtils.deleteDirectory(propertiesDirectory);
    }

    protected static void backupConfigurationFile() throws IOException {
        if (propertiesSourceFile.exists()) {
            FileUtils.copyFile(propertiesSourceFile, propertiesBackupFile);
        }
    }

    private static void restoreConfigurationFile() throws IOException {
        if (propertiesBackupFile.exists()) {
            FileUtils.copyFile(propertiesBackupFile, propertiesSourceFile);
        }
    }

    protected static void resetConfiguration() throws IOException {
        restoreConfigurationFile();
        deleteConfigurationFolder();
    }

    protected void setServer(MockTCPServer server) {
        if (server == null && this.server != null) {
            this.server.close();
        }

        this.server = server;
    }

    protected void close() throws IOException {
        this.setClient(null);
        this.setServer(null);
    }
}