/**
================================================================================

   Project: Procter and Gamble - Skelmersdale.

   $HeadURL$

   $Author$

   $Revision$

   $Date$

$Log$

============================== (c) Swisslog(UK) Ltd, 2005 ======================
*/
package io.cloudracer;

import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.core.Appender;

import io.cloudracer.mocktcpserver.MockTCPServer;
import io.cloudracer.mocktcpserver.tcpclient.TCPClient;

/**
 * Tools <b>exclusively</b> for use by test routines.
 *
 * @author John McDonnell
 */
public abstract class AbstractTestTools extends TestConstants {

    private TCPClient client;
    private MockTCPServer server;

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
