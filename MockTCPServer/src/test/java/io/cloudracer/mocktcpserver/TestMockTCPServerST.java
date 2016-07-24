package io.cloudracer.mocktcpserver;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.cloudracer.AbstractTestTools;
import io.cloudracer.TestConstants;
import io.cloudracer.mocktcpserver.tcpclient.TCPClient;

/**
 * Mock TCP Server tests.
 *
 * @author John McDonnell
 */
public class TestMockTCPServerST extends AbstractTestTools {

    // Print to the console only so that the LogMonitor does not interpret it as an error.
    private final Logger logger = LogManager.getLogger(TestMockTCPServerST.class);

    private static final int TIMEOUT = 10000;

    MockTCPServer server1111;
    MockTCPServer server2222;

    final List<TCPClient> clientList = new ArrayList<>();

    @Override
    @Before
    public void setUp() throws IOException, ConfigurationException, InterruptedException {
        super.setUp();

        getServer().setIsSendResponses(false);

        server1111 = getServerFactory(TestConstants.MOCK_SERVER_PORT_1111, true);
        server1111.setIsSendResponses(false);
        server2222 = getServerFactory(TestConstants.MOCK_SERVER_PORT_2222, true);
        server2222.setIsSendResponses(false);
    }

    @Override
    @After
    public void cleanUp() throws IOException {
        super.cleanUp();

        // Close all open clients and remove them from the list.
        for (Iterator<TCPClient> iterator = clientList.iterator(); iterator.hasNext();) {
            TCPClient tcpClient = iterator.next();
            tcpClient.close();
            iterator.remove();
        }

        server1111.close();
        server2222.close();

        this.checkLogMonitorForUnexpectedMessages();
    }

    /**
     * Server returns the expected ACK to a properly terminated message.
     *
     * @throws IOException see source documentation.
     * @throws ConfigurationException error reading the configuration file
     * @throws InterruptedException the MockTCPServer was unexpectedly interrupted
     */
    @Test(timeout = TIMEOUT)
    public void ack() throws IOException, ConfigurationException, InterruptedException {
        final int totalClientsPerServer = 1000;

        // Start the client connections and send two messages from each one.
        for (int i = 0; i < totalClientsPerServer; i++) {
            clientList.add(clientList.size(), getClientFactory(TestConstants.MOCK_SERVER_PORT_1111));
            assertArrayEquals(TestConstants.getAck(), clientList.get(clientList.size() - 1).send(TestConstants.WELLFORMED_XML_WITH_VALID_TERMINATOR).toByteArray());
            assertArrayEquals(TestConstants.getAck(), clientList.get(clientList.size() - 1).send(TestConstants.WELLFORMED_XML_WITH_VALID_TERMINATOR).toByteArray());
            clientList.add(clientList.size(), getClientFactory(TestConstants.MOCK_SERVER_PORT_2222));
            assertArrayEquals(TestConstants.getAck(), clientList.get(clientList.size() - 1).send(TestConstants.WELLFORMED_XML_WITH_VALID_TERMINATOR).toByteArray());
            assertArrayEquals(TestConstants.getAck(), clientList.get(clientList.size() - 1).send(TestConstants.WELLFORMED_XML_WITH_VALID_TERMINATOR).toByteArray());
        }

        // Confirm that each client is still open.
        for (TCPClient tcpClient : clientList) {
            assertTrue(tcpClient.isConectionActive());
        }

        this.checkLogMonitorForUnexpectedMessages();
    }

    /**
     * Having set a customised terminator, the server returns the expected ACK to a message terminated with the custom terminator.
     *
     * @throws IOException see source documentation.
     * @throws InterruptedException
     * @throws ConfigurationException
     */
    @Test(timeout = TIMEOUT)
    public void ackWithCustomTerminator() throws IOException, InterruptedException, ConfigurationException {
        final byte[] customTerminator = new byte[] { 88, 89, 90 }; // XYZ
        final String message = String.format("%s%s", TestConstants.WELLFORMED_XML, new String(customTerminator));

        // Set the custom terminator.
        this.getServer().setTerminator(customTerminator);

        // Send a message with an incorrect terminator (i.e. the default, that we just changed) and wait for the response.
        final Thread waitForResponse = new Thread("WaitForResponse") {

            @Override
            public void run() {
                try {
                    TestMockTCPServerST.this.getClient().send(TestConstants.WELLFORMED_XML_WITH_VALID_TERMINATOR);
                } catch (final IOException e) {
                    TestMockTCPServerST.this.logger.error(e.getMessage(), e);
                }
            }
        };
        waitForResponse.start();

        // Wait to confirm that the message read is not terminated as the Client has not returned.
        final int timeout = 5000; // 5 seconds.
        waitForResponse.join(timeout);
        assertTrue(waitForResponse.isAlive());

        // Reset the Client.
        this.setClient(null);
        waitForResponse.join(timeout);
        assertFalse(waitForResponse.isAlive());

        // Send a message with the correct terminator (i.e. the custom one we set at the start of this method).
        assertArrayEquals(TestConstants.getAck(), this.getClient().send(message).toByteArray());

        this.checkLogMonitorForUnexpectedMessages();
    }

    /**
     * Having set the Server to always return a NAK, the Server returns the expected NAK when an ACK would normally be expected.
     *
     * @throws IOException see source documentation.
     * @throws InterruptedException
     * @throws ConfigurationException
     */
    @Test(timeout = TIMEOUT)
    public void forceNAK() throws IOException, ConfigurationException, InterruptedException {
        assertArrayEquals(TestConstants.getAck(), this.getClient().send(TestConstants.WELLFORMED_XML_WITH_VALID_TERMINATOR).toByteArray());

        this.getClient().close();
        this.getServer().setIsAlwaysNAKResponse(true);

        assertArrayEquals(TestConstants.getNak(), this.getClient().send(TestConstants.WELLFORMED_XML_WITH_VALID_TERMINATOR).toByteArray());

        this.checkLogMonitorForUnexpectedMessages();
    }

    /**
     * Having set the Server to never respond, wait for the Server {@link Thread} to die. If the server has not responded after 5 seconds, assume that it never will.
     *
     * @throws InterruptedException see source documentation.
     * @throws ConfigurationException
     */
    @Test(timeout = TIMEOUT)
    public void forceNoResponse() throws InterruptedException, ConfigurationException {
        this.getServer().setIsAlwaysNoResponse(true);

        final Thread waitForResponse = new Thread("WaitForResponse") {

            @Override
            public void run() {
                try {
                    TestMockTCPServerST.this.getClient().send(TestConstants.WELLFORMED_XML_WITH_VALID_TERMINATOR);
                } catch (final IOException e) {
                    TestMockTCPServerST.this.logger.error(e.getMessage(), e);
                }
            }
        };
        waitForResponse.start();

        final int timeout = 5000; // 5 seconds.
        waitForResponse.join(timeout);
        assertTrue(waitForResponse.isAlive());

        this.checkLogMonitorForUnexpectedMessages();
    }

    /**
     * Server responses can be ignored; the client will not wait for the Server to respond.
     *
     * @throws IOException see source documentation.
     */
    @Test(timeout = TIMEOUT)
    public void ignoreResponse() throws IOException {
        assertArrayEquals(TestConstants.getAck(), this.getClient().send(TestConstants.WELLFORMED_XML_WITH_VALID_TERMINATOR).toByteArray());

        assertNull(this.getClient().send(TestConstants.WELLFORMED_XML_WITH_VALID_TERMINATOR, false));

        this.checkLogMonitorForUnexpectedMessages();
    }

    /**
     * Having set the Server to expect only messages that match a specified Regular Expression, ensure that a NAK is returned for messages that do not match and an ACK for messages that do match.
     *
     * @throws IOException see source documentation.
     * @throws InterruptedException
     * @throws ConfigurationException
     */
    @Test(timeout = TIMEOUT)
    public void expectSpecificMessage() throws IOException, ConfigurationException, InterruptedException {
        final String baseMessage = "Hello World!!";
        final String message = String.format("%s%s", baseMessage, new String(TestConstants.DEFAULT_TERMINATOR));

        // All messages sent to the Server must match this regular expression.
        final String messageRegularExpression = String.format("%s%s", "Hello.*", new String(TestConstants.DEFAULT_TERMINATOR));
        final String invalidMessage = String.format("%s%s", "This does not match the expected Regular Expression.",
                new String(TestConstants.DEFAULT_TERMINATOR));

        this.getServer().setExpectedMessage(messageRegularExpression);

        assertArrayEquals(TestConstants.getAck(), this.getClient().send(message).toByteArray());
        assertNull(this.getServer().getAssertionError());
        assertArrayEquals(TestConstants.getNak(), this.getClient().send(invalidMessage).toByteArray());
        assertNotNull(this.getServer().getAssertionError());

        this.checkLogMonitorForUnexpectedMessages();
    }
}