package io.cloudracer;

import static org.junit.Assert.assertThat;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import org.apache.log4j.Logger;

import pg.HostMessageConstants;
import pg.HostMessageTypeConstants;
import pg.RegexMatcher;

/**
 * A mock server used for testing purposes only.
 *
 * @author v7mcdoj
 */
public class MockTCPServer extends Thread {

    private Logger logger = Logger.getLogger(this.getClass().getSimpleName());

    private AssertionError assertionError;

    private ServerSocket socket;
    private BufferedReader inputStream;
    private DataOutputStream outputStream;
    private RegexMatcher expectedMessage;

    private static int port;
    private boolean setIsAlwaysNAKResponse = false;
    private boolean setIsAlwaysNoResponse = false;
    private boolean isCloseAfterNextResponse = false;
    private int messagesReceivedCount = 0;

    public MockTCPServer(final int port) {
        logger.info("Mock Host Server starting...");

        super.setName(String.format("Mock-TCP-Server-%d", port));

        setPort(port);
        start();
    }

    @Override
    public void run() {
        try {
            setSocket(new ServerSocket(getPort()));
            while (getSocket() != null && !getSocket().isClosed()) {
                logger.info("Mock Host Server waiting for a connection...");
                final Socket connectionSocket = getSocket().accept();
                logger.info("Mock Host Server connected.");
                setInputStream(new BufferedReader(new InputStreamReader(connectionSocket.getInputStream())));
                setOutputStream(new DataOutputStream(connectionSocket.getOutputStream()));
                final StringBuffer messageBuffer = new StringBuffer();
                final byte[] nextByte = new byte[1];
                int c;
                logger.info("Mock Host Server ready to receive input.");
                while ((c = getInputStream().read()) != -1) {
                    nextByte[0] = (byte) c;
                    messageBuffer.append(new String(nextByte, "UTF-8"));
                    if (messageBuffer.toString().endsWith(HostMessageTypeConstants.VALID_TERMINATOR)) {
                        incrementMessagesReceivedCount();

                        break;
                    }
                }
                // Ignore null in order allow a probing ping e.g. paping.exe
                if (messageBuffer != null) {
                    setAssertionError(null);
                    try {
                        if (getExpectedMessage() != null) {
                            assertThat("Unexpected message from the AM Host Client.", messageBuffer.toString(), getExpectedMessage());
                        }
                    } catch (AssertionError e) {
                        setAssertionError(e);
                    }
                    onMessage(messageBuffer);
                    String response = null;
                    if (!getIsAlwaysNoResponse()) {
                        if (getAssertionError() == null && !getIsAlwaysNAKResponse()) {
                            response = HostMessageConstants.ACK;
                        } else {
                            response = HostMessageConstants.NAK;
                        }

                        getOutputStream().writeBytes(response);

                        afterResponse(response);
                    }

                    setOutputStream(null);
                }
            }
        } catch (SocketException e) {
            logger.warn(e.getMessage());
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * A server callback when a message has been processed, and a response has been sent to the client.
     *
     * @param response the response that has been sent.
     * @throws IOException
     */
    public void afterResponse(final String response) throws IOException {
        logger.info(String.format("Mock Host Server sent the response: %s.", response));

        if (getIsCloseAfterNextResponse()) {
            close();
        }
    }

    /**
     * A server callback when a message is received.
     *
     * @param message the message received.
     */
    public void onMessage(final StringBuffer message) {
        logger.info(String.format("Mock Host Server received: %s.", message));
    }

    /**
     * An error is recorded if a message other than that which is {@link MockTCPServer#getAssertionError() expected} is received.
     *
     * @return a recorded error.
     */
    public AssertionError getAssertionError() {
        return this.assertionError;
    }

    /**
     * An error will be recorded if a message other than that which is {@link MockTCPServer#getAssertionError() expected} is received.
     *
     * @param assertionError a recorded error.
     */
    public void setAssertionError(AssertionError assertionError) {
        this.assertionError = assertionError;
    }

    /**
     * Forces the Server to return a NAK in response to the next message received (regardless of <u>any</u> other conditions). The next message will first be processed as normal; irrespective of this property.
     * <p>
     * This is intended to be used to test a clients response to receiving a NAK.
     * <p>
     * Default is false.
     *
     * @return If true, the Servers next response will always be a NAK.
     */
    public boolean getIsAlwaysNAKResponse() {
        return this.setIsAlwaysNAKResponse;
    }

    /**
     * Forces the Server to return a NAK in response to the next message received (regardless of <u>any</u> other conditions). The next message will first be processed as normal; irrespective of this property.
     * <p>
     * This is intended to be used to test a clients response to receiving a NAK.
     * <p>
     * Default is false.
     *
     * @param isCloseAfterNextResponse if true, the Servers next response will always be a NAK.
     */
    public void setIsAlwaysNAKResponse(final boolean isAlwaysNAKResponse) {
        this.setIsAlwaysNAKResponse = isAlwaysNAKResponse;
    }

    public boolean getIsAlwaysNoResponse() {
        return setIsAlwaysNoResponse;
    }

    public void setIsAlwaysNoResponse(final boolean isAlwaysNoResponse) {
        this.setIsAlwaysNoResponse = isAlwaysNoResponse;
    }

    /**
     * Forces the Server to close down after processing the next message received (regardless of <u>any</u> other conditions). The next message will first be processed as normal; irrespective of this property.
     * <p>
     * This is intended to be used so that test clients can wait on the server Thread to end.
     * <p>
     * Default is false.
     *
     * @return if true, the Server will close after the message processing is complete.
     */
    public boolean getIsCloseAfterNextResponse() {
        return isCloseAfterNextResponse;
    }

    /**
     * Forces the Server to close down after processing the next message received (regardless of <u>any</u> other conditions). The next message will first be processed as normal; irrespective of this property.
     * <p>
     * This is intended to be used so that test clients can wait on the server Thread to end.
     * <p>
     * Default is false.
     *
     * @param isCloseAfterNextResponse if true, the Server will close after the message processing is complete.
     */
    public void setIsCloseAfterNextResponse(boolean isCloseAfterNextResponse) {
        this.isCloseAfterNextResponse = isCloseAfterNextResponse;
    }

    /**
     * If any message, other that this one, is the next message to be received, record it as an {@link MockTCPServer#setAssertionError(AssertionError) assertion error}.
     *
     * @return ignore if null.
     */
    public RegexMatcher getExpectedMessage() {
        return expectedMessage;
    }

    /**
     * If any message, other that this one, is the next message to be received, record it as an {@link MockTCPServer#setAssertionError(AssertionError) assertion error}.
     *
     * @param expectedMessage a Regular Expression that describes what the next received message will be.
     */
    public void setExpectedMessage(final String expectedMessage) {
        this.expectedMessage = new RegexMatcher(expectedMessage);
    }

    /**
     * If any message, other that this one, is the next message to be received, record it as an {@link MockTCPServer#setAssertionError(AssertionError) assertion error}.
     *
     * @param expectedMessage a Regular Expression that describes what the next received message will be.
     */
    public void setExpectedMessage(final StringBuffer expectedMessage) {
        setExpectedMessage(expectedMessage.toString());
    }

    public int getMessagesReceivedCount() {
        return messagesReceivedCount;
    }

    private void incrementMessagesReceivedCount() {
        this.messagesReceivedCount++;
    }

    /**
     * Close the socket (if it is open) and any open data streams.
     *
     * @throws IOException
     */
    public void close() throws IOException {
        logger.info("Mock Host Server stopping...");

        setOutputStream(null);
        setInputStream(null);
        setSocket(null);
    }

    private static int getPort() {
        return port;
    }

    private static void setPort(int port) {
        MockTCPServer.port = port;
    }

    private ServerSocket getSocket() {
        return socket;
    }

    private void setSocket(ServerSocket socket) {
        if (socket == null && this.socket != null) {
            try {
                logger.info("Mock Host closing the server socket...");
                this.socket.close();
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }

        this.socket = socket;
    }

    private BufferedReader getInputStream() {
        return inputStream;
    }

    private void setInputStream(BufferedReader inputStream) throws IOException {
        if (inputStream == null && this.inputStream != null) {
            logger.info("Mock Host closing the input stream...");
            this.inputStream.close();
        }

        this.inputStream = inputStream;
    }

    private DataOutputStream getOutputStream() {
        return outputStream;
    }

    private void setOutputStream(DataOutputStream outputStream) throws IOException {
        if (outputStream == null && this.outputStream != null) {
            logger.info("Mock Host flushing the output stream...");
            this.outputStream.flush();
            logger.info("Mock Host closing the output stream...");
            this.outputStream.close();
        }

        this.outputStream = outputStream;
    }
}