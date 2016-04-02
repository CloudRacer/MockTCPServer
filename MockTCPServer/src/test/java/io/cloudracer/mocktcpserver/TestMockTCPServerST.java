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

/**
 * Mock TCP Server tests.
 *
 * @author John McDonnell
 */
public class TestMockTCPServerST extends AbstractTestTools {

    private final static Logger logger = LogManager.getLogger();

    private static final int TIMEOUT = 10000;

    @Override
    @Before
    public void setUp() throws IOException {
        super.setUp();
    }

    @Override
    @After
    public void cleanUp() throws IOException {
        super.cleanUp();
    }

    /**
     * Server returns the expected ACK to a properly terminated message.
     *
     * @throws ClassNotFoundException see source documentation.
     * @throws IOException see source documentation.
     */
    @Test(timeout = TIMEOUT)
    public void ack() throws ClassNotFoundException, IOException {
        assertArrayEquals(ACK, this.getClient().send(WELLFORMED_XML_WITH_VALID_TERMINATOR).toByteArray());

        this.checkLogMonitorForUnexpectedMessages();
    }

    /**
     * Having set a customised terminator, the server returns the expected ACK to a message terminated with the custom terminator.
     *
     * @throws ClassNotFoundException see source documentation.
     * @throws IOException see source documentation.
     * @throws InterruptedException
     */
    @Test(timeout = TIMEOUT)
    public void ackWithCustomTerminator() throws ClassNotFoundException, IOException, InterruptedException {
        final byte[] customTerminator = new byte[] { 88, 89, 90 }; // XYZ
        final String message = String.format("%s%s", WELLFORMED_XML, new String(customTerminator));

        // Set the custom terminator.
        this.getServer().setTerminator(customTerminator);

        // Send a message with an incorrect terminator (i.e. the default, that we just changed) and wait for the response.
        final Thread waitForResponse = new Thread("WaitForResponse") {
            @Override
            public void run() {
                try {
                    TestMockTCPServerST.this.getClient().send(WELLFORMED_XML_WITH_VALID_TERMINATOR);
                } catch (final ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (final IOException e) {
                    e.printStackTrace();
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
        assertArrayEquals(ACK, this.getClient().send(message).toByteArray());

        this.checkLogMonitorForUnexpectedMessages();
    }

    /**
     * Having set the Server to always return a NAK, the Server returns the expected NAK when an ACK would normally be expected.
     *
     * @throws ClassNotFoundException see source documentation.
     * @throws IOException see source documentation.
     */
    @Test(timeout = TIMEOUT)
    public void forceNAK() throws ClassNotFoundException, IOException {
        assertArrayEquals(ACK, this.getClient().send(WELLFORMED_XML_WITH_VALID_TERMINATOR).toByteArray());

        this.getServer().setIsAlwaysNAKResponse(true);

        assertArrayEquals(NAK, this.getClient().send(WELLFORMED_XML_WITH_VALID_TERMINATOR).toByteArray());

        this.checkLogMonitorForUnexpectedMessages();
    }

    /**
     * Having set the Server to close after the next response, wait for the Server {@link Thread} to die after sending one message.
     *
     * @throws ClassNotFoundException see source documentation.
     * @throws IOException see source documentation.
     * @throws InterruptedException see source documentation.
     */
    @Test(timeout = TIMEOUT)
    public void forceCloseAfterNextResponse() throws ClassNotFoundException, IOException, InterruptedException {
        this.getServer().setIsCloseAfterNextResponse(true);

        assertArrayEquals(ACK, this.getClient().send(WELLFORMED_XML_WITH_VALID_TERMINATOR).toByteArray());

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
    public void forceAlwaysNoResponse() throws InterruptedException {
        this.getServer().setIsAlwaysNoResponse(true);

        final Thread waitForResponse = new Thread("WaitForResponse") {
            @Override
            public void run() {
                try {
                    TestMockTCPServerST.this.getClient().send(WELLFORMED_XML_WITH_VALID_TERMINATOR);
                } catch (final ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (final IOException e) {
                    e.printStackTrace();
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
     * @throws ClassNotFoundException see source documentation.
     * @throws IOException see source documentation.
     */
    @Test(timeout = TIMEOUT)
    public void ignoreResponse() throws ClassNotFoundException, IOException {
        assertArrayEquals(ACK, this.getClient().send(WELLFORMED_XML_WITH_VALID_TERMINATOR).toByteArray());

        assertNull(this.getClient().send(WELLFORMED_XML_WITH_VALID_TERMINATOR, false));

        this.checkLogMonitorForUnexpectedMessages();
    }

    /**
     * Having set the Server to expect only messages that match a specified Regular Expression, ensure that a NAK is returned for messages that do not match and an ACK for messages that do match.
     *
     * @throws ClassNotFoundException see source documentation.
     * @throws IOException see source documentation.
     */
    @Test(timeout = TIMEOUT)
    public void expectSpecificMessage() throws ClassNotFoundException, IOException {
        final String baseMessage = "Hello World!!";
        final String message = String.format("%s%s", baseMessage, new String(DEFAULT_TERMINATOR));

        // All messages sent to the Server must match this regular expression.
        final String messageRegularExpression = String.format("%s%s", "Hello.*", new String(DEFAULT_TERMINATOR));
        final String invalidMessage = String.format("%s%s", "This does not match the expected Regular Expression.",
                new String(DEFAULT_TERMINATOR));

        this.getServer().setExpectedMessage(messageRegularExpression);

        assertArrayEquals(ACK, this.getClient().send(message).toByteArray());
        assertNull(this.getServer().getAssertionError());
        assertArrayEquals(NAK, this.getClient().send(invalidMessage).toByteArray());
        assertNotNull(this.getServer().getAssertionError());

        this.checkLogMonitorForUnexpectedMessages();
    }
}