package io.cloudracer.mocktcpserver;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.cloudracer.AbstractTestTools;
import io.cloudracer.TestConstants;

/**
 * Mock TCP Server tests.
 *
 * @author John McDonnell
 */
public class TestMockTCPServerST extends AbstractTestTools {

    // Print to the console only so that the LogMonitor does not interpret it as an error.
    private final Logger logger = LogManager.getLogger(TestMockTCPServerST.class);

    private static final int TIMEOUT = 10000;

    @Override
    @Before
    public void setUp() throws IOException {
        super.setUp();

        this.getServer().setIsSendResponses(false);
    }

    @Override
    @After
    public void cleanUp() throws IOException {
        super.cleanUp();
    }

    /**
     * Server returns the expected ACK to a properly terminated message.
     *
     * @throws IOException see source documentation.
     */
    @Test(timeout = TIMEOUT)
    public void ack() throws IOException {
        assertArrayEquals(TestConstants.getAck(), this.getClient().send(TestConstants.WELLFORMED_XML_WITH_VALID_TERMINATOR).toByteArray());

        this.checkLogMonitorForUnexpectedMessages();
    }

    /**
     * Having set a customised terminator, the server returns the expected ACK to a message terminated with the custom terminator.
     *
     * @throws IOException see source documentation.
     * @throws InterruptedException
     */
    @Test(timeout = TIMEOUT)
    public void ackWithCustomTerminator() throws IOException, InterruptedException {
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

        // Wait to confirm that the response is not received as the Client Thread is still alive.
        final int timeout = 5000; // 5 seconds.
        waitForResponse.join(timeout);
        assertTrue(waitForResponse.isAlive());

        // Reset the Client.
        this.setClient(null);
        waitForResponse.join(timeout);
        assertFalse(waitForResponse.isAlive());

        // Send a message with the correct terminator (i.e. the custom on we set at the start of this method) and wait for the response.
        assertArrayEquals(TestConstants.getAck(), this.getClient().send(message).toByteArray());

        this.checkLogMonitorForUnexpectedMessages();
    }

    /**
     * Having set the Server to always return a NAK, the Server returns the expected NAK when an ACK would normally be expected.
     *
     * @throws IOException see source documentation.
     */
    @Test(timeout = TIMEOUT)
    public void forceNAK() throws IOException {
        assertArrayEquals(TestConstants.getAck(), this.getClient().send(TestConstants.WELLFORMED_XML_WITH_VALID_TERMINATOR).toByteArray());

        this.getServer().setIsAlwaysNAKResponse(true);

        assertArrayEquals(TestConstants.getNak(), this.getClient().send(TestConstants.WELLFORMED_XML_WITH_VALID_TERMINATOR).toByteArray());

        this.checkLogMonitorForUnexpectedMessages();
    }

    /**
     * Having set the Server to close after the next response, wait for the Server {@link Thread} to die after sending one message.
     *
     * @throws IOException see source documentation.
     * @throws InterruptedException see source documentation.
     */
    @Test(timeout = TIMEOUT)
    public void forceCloseAfterNextResponse() throws IOException, InterruptedException {
        this.getServer().setIsCloseAfterNextResponse(true);

        assertArrayEquals(TestConstants.getAck(), this.getClient().send(TestConstants.WELLFORMED_XML_WITH_VALID_TERMINATOR).toByteArray());

        // Wait for the MockTCPServer to Thread to die.
        final int timeout = 5000; // 5 seconds.
        this.getServer().join(timeout);

        assertFalse(this.getServer().isAlive());

        this.checkLogMonitorForUnexpectedMessages();
    }

    /**
     * Having set the Server to never respond, wait for the Server {@link Thread} to die. If the server has not responded after 5 seconds, assume that it never will.
     *
     * @throws InterruptedException see source documentation.
     */
    @Test(timeout = TIMEOUT)
    public void forceNoResponse() throws InterruptedException {
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
     */
    @Test(timeout = TIMEOUT)
    public void expectSpecificMessage() throws IOException {
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