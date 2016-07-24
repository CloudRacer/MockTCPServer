package io.cloudracer;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;

import io.cloudracer.mocktcpserver.MockTCPServer;
import io.cloudracer.mocktcpserver.datastream.DataStream;
import io.cloudracer.mocktcpserver.tcpclient.TCPClient;
import io.cloudracer.properties.ConfigurationSettings;

/**
 * Tools <b>exclusively</b> for use by test routines.
 *
 * @author John McDonnell
 */
public abstract class AbstractTestTools {

    private static final Logger logger = LogManager.getLogger();

    private static final File propertiesSourceFile = new File(TestConstants.MOCKTCPSERVER_XML_RESOURCE_TARGET_FILE_NAME);
    private static final File propertiesBackupFile = new File(String.format("%s%s%s-backup.%s", propertiesSourceFile.getParent(), File.separatorChar, FilenameUtils.getBaseName(propertiesSourceFile.getName()), FilenameUtils.getExtension(propertiesSourceFile.getName())));

    private TCPClient client;
    private MockTCPServer server;
    private ConfigurationSettings configurationSettings;

    protected static void initialiseSystemProperties() throws IOException {
        System.getProperties().remove(TestConstants.CONFIGURATION_INITIALISATION_ENABLED_PROPERTY_NAME);

        logger.debug(String.format("Properties: %s.", StringUtils.join(System.getProperties())));
    }

    protected void setUp() throws IOException, ConfigurationException, InterruptedException {
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
            Thread.currentThread().interrupt();
        }
        assertNull(String.format("An unexpected message was logged to the file \"%s\".", LogMonitor.getFileName()), LogMonitor.getLastEventLogged());
    }

    protected void resetLogMonitor() {
        LogMonitor.setLastEventLogged(null);
    }

    protected TCPClient getClient() throws IOException {
        if (this.client == null) {
            this.client = getClientFactory(TestConstants.MOCK_SERVER_PORT_6789);
        }

        return this.client;
    }

    protected TCPClient getClient(final int port) throws IOException {
        if (this.client == null) {
            this.client = getClientFactory(port);
        }

        return this.client;
    }

    protected TCPClient getClientFactory(final int port) throws IOException {
        return new TCPClient(port);
    }

    protected void setClient(TCPClient client) throws IOException {
        if (client == null && this.client != null) {
            this.client.close();
        }

        this.client = client;
    }

    protected MockTCPServer getServer() throws ConfigurationException, InterruptedException {
        return getServer(TestConstants.MOCK_SERVER_PORT_6789, true);
    }

    protected MockTCPServer getServer(final boolean start) throws ConfigurationException, InterruptedException {
        return getServer(TestConstants.MOCK_SERVER_PORT_6789, start);
    }

    protected MockTCPServer getServer(final int port) throws ConfigurationException, InterruptedException {
        return getServer(port, true);
    }

    protected MockTCPServer getServer(final int port, final boolean start) throws ConfigurationException, InterruptedException {
        if (this.server == null) {
            this.server = getServerFactory(port, start);
        }

        return this.server;
    }

    protected MockTCPServer getServerFactory(int port, final boolean start) throws ConfigurationException, InterruptedException {
        return new MockTCPServer(port, start);
    }

    protected ConfigurationSettings getConfigurationSettings() {
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

    protected void testResponses(final int responseListenerPort, final String message, final List<String> expectedMessages, final int timeout, final int retryInterval) throws IOException, InterruptedException, ConfigurationException {
        testResponses(getServer(), responseListenerPort, message, expectedMessages, timeout, retryInterval);
    }

    protected void testResponses(final MockTCPServer server, final int responseListenerPort, final String message, final List<String> expectedMessages, final int timeout, final int retryInterval) throws IOException, InterruptedException, ConfigurationException {
        final List<String> actualMessages = new ArrayList<>();
        final MockTCPServer mockTCPServer = new MockTCPServer(responseListenerPort) {

            @Override
            public void onMessage(DataStream message) {
                actualMessages.add(message.toString());

                super.onMessage(message);

                if (actualMessages.size() == expectedMessages.size()) {
                    close();
                }
            }
        };
        final TCPClient tcpClient = new TCPClient(server.getPort());
        assertArrayEquals(TestConstants.getAck(), tcpClient.send(message).toByteArray());
        tcpClient.close();
        // Will close when the expected number of messages are received.
        mockTCPServer.join();
        server.close();

        int i = 0;
        while (server.isAlive()) {
            server.join(retryInterval);

            assertFalse(String.format("Timed out waiting for the Server listening to port %d to close.", mockTCPServer.getPort()), !(i < timeout));

            i = i + (retryInterval);
        }

        /**
         * Messages can arrive an any order so test that each, and every, received message is an expected one.
         * This may mean that duplicates can cause a false positive under certain, albeit unlikely, circumstances. If the expected list does not contain duplicates of more than one message, such duplicates do not pose a problem.
         */
        assertEquals(expectedMessages.size(), actualMessages.size());
        for (String messageReceived : actualMessages) {
            expectedMessages.contains(messageReceived);
        }

        mockTCPServer.close();
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