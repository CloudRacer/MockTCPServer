package io.cloudracer.mocktcpserver;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;

import io.cloudracer.mocktcpserver.datastream.DataStream;
import io.cloudracer.mocktcpserver.datastream.DataStreamRegexMatcher;

/**
 * A TCP Server that is designed to simulate success <b>and</b> failure conditions in System/Integration test environments.
 *
 * @author John McDonnell
 */
public class MockTCPServer extends Thread implements Closeable {

    private final Logger logger = LogManager.getLogger(getRootLoggerName());

    private final static int DEFAULT_PORT = 6789;
    private final static byte[] DEFAULT_TERMINATOR = { 13, 10, 10 };
    private final static byte[] DEFAULT_ACK = { 65 };
    private final static byte[] DEFAULT_NAK = { 78 };

    private byte[] terminator = null;
    private byte[] ack = null;
    private byte[] nak = null;

    private AssertionError assertionError;

    private ServerSocket socket;
    private BufferedReader inputStream;
    private DataOutputStream outputStream;
    private DataStreamRegexMatcher expectedMessage;

    private DataStream dataStream;
    private Socket connectionSocket;

    private int port;
    private boolean setIsAlwaysNAKResponse = false;
    private boolean setIsAlwaysNoResponse = false;
    private boolean isCloseAfterNextResponse = false;
    private int messagesReceivedCount = 0;

    private boolean isClosed = false;

    /**
     * Start the server on the {@link MockTCPServer#DEFAULT_PORT default} port.
     *
     * This constructor is the equivalent of passing the {@link MockTCPServer#DEFAULT_PORT default} port to the other {@link MockTCPServer#MockTCPServer(int) constructor}.
     */
    public MockTCPServer() {
        this(MockTCPServer.DEFAULT_PORT);
    }

    /**
     * Start the server on the specified port.
     *
     * @param port
     *        the TCP Server will listen on this port.
     */
    public MockTCPServer(final int port) {
        this.logger.info("Starting...");

        super.setName(String.format("%s-%d", getThreadName(), port));

        setPort(port);
        start();
        /*
         * If this pause is not done here, a test that *immediately* tries to connect, may get a "connection refused" error.
         */
        try {
            TimeUnit.MILLISECONDS.sleep(20);
        } catch (final InterruptedException e) {
            // Do nothing.
        }
    }

    @Override
    public void run() {
        try {
            while (!isClosed() && (getSocket() != null)) {
                try {
                    setDataStream(null);
                    while ((getDataStream().write(getInputStream().read())) != -1) {
                        if (Arrays.equals(getDataStream().getTail(), getTerminator())) {
                            incrementMessagesReceivedCount();

                            break;
                        }
                    }
                    // Ignore null in order allow a probing ping e.g. paping.exe
                    if (getDataStream().size() > 0) {
                        setAssertionError(null);
                        try {
                            if (getExpectedMessage() != null) {
                                Assert.assertThat("Unexpected message from the AM Host Client.", getDataStream(), getExpectedMessage());
                            }
                        } catch (final AssertionError e) {
                            setAssertionError(e);
                        }
                        onMessage(getDataStream());
                        // If the stream has not ended and a response is required, send one.
                        if ((getDataStream().getLastByte() != -1) && !getIsAlwaysNoResponse()) {
                            byte[] response = null;

                            if ((getAssertionError() == null) && !getIsAlwaysNAKResponse()) {
                                response = getACK();
                            } else {
                                response = getNAK();
                            }

                            getOutputStream().write(response);

                            afterResponse(response);
                        }
                    }
                } catch (final SocketException e) {
                    handleException(e);
                } catch (final IOException e) {
                    handleException(e);
                } catch (final Exception e) {
                    handleException(e);
                }
            }
        } catch (final SocketException e) {
            handleException(e);
        } catch (final IOException e) {
            handleException(e);
        } catch (final Exception e) {
            handleException(e);
        } finally {
            close();
        }
    }

    private void handleException(final Exception e) {
        if (e.getMessage().toLowerCase().equals("socket closed")) {
            this.logger.warn(e.getMessage());
        } else if (e.getMessage().toLowerCase().equals("socket input is shutdown")) {
            this.logger.warn(e.getMessage());
        } else if (e.getMessage().toLowerCase().equals("socket output is shutdown")) {
            this.logger.warn(e.getMessage());
        } else if (e.getMessage().toLowerCase().equals("socket is closed")) {
            this.logger.warn(e.getMessage());
        } else if (e.getMessage().toLowerCase().equals("stream closed")) {
            this.logger.warn(e.getMessage());
        } else if (e.getMessage().toLowerCase().equals("connection reset")) {
            this.logger.warn(e.getMessage());
        } else {
            this.logger.error(e.getMessage(), e);
        }
    }

    /**
     * The server will read the stream until these characters are encountered.
     *
     * @return the terminator.
     */
    public byte[] getTerminator() {
        if (this.terminator == null) {
            this.terminator = MockTCPServer.DEFAULT_TERMINATOR;
        }

        return this.terminator;
    }

    /**
     * The server will read the stream until these characters are encountered.
     *
     * @param terminator
     *        the terminator.
     */
    public synchronized void setTerminator(final byte[] terminator) {
        this.terminator = terminator;
    }

    /**
     * The <b>positive</b> acknowledgement response.
     *
     * @return positive acknowledgement
     */
    public byte[] getACK() {
        if (this.ack == null) {
            this.ack = MockTCPServer.DEFAULT_ACK;
        }

        return this.ack;
    }

    /**
     * The <b>positive</b> acknowledgement response.
     *
     * @param ack
     *        positive acknowledgement
     */
    public synchronized void setACK(final byte[] ack) {
        this.ack = ack;
    }

    /**
     * The <b>negative</b> acknowledgement response.
     *
     * @return negative acknowledgement
     */
    public byte[] getNAK() {
        if (this.nak == null) {
            this.nak = MockTCPServer.DEFAULT_NAK;
        }

        return this.nak;
    }

    /**
     * The <b>negative</b> acknowledgement response.
     *
     * @param nak
     *        negative acknowledgement
     */
    public synchronized void setNAK(final byte[] nak) {
        this.nak = nak;
    }

    /**
     * A server callback when a message has been processed, and a response has been sent to the client.
     *
     * @param response
     *        the response that has been sent.
     */
    public synchronized void afterResponse(final byte[] response) {
        this.logger.debug(String.format("Sent the response: %s.", new String(response)));

        if (getIsCloseAfterNextResponse()) {
            setClosed(true);
        }
    }

    /**
     * A server callback when a message is received.
     *
     * @param message
     *        the message received.
     */
    public void onMessage(final DataStream message) {
        this.logger.info(String.format("Received: %s.", message.toString()));
    }

    /**
     * An error is recorded if a message other than that which is expected is received.
     *
     * @return a recorded error.
     */
    public AssertionError getAssertionError() {
        return this.assertionError;
    }

    /**
     * An error will be recorded if a message other than that which is {@link MockTCPServer#getAssertionError() expected} is received.
     *
     * @param assertionError
     *        a recorded error.
     */
    private void setAssertionError(final AssertionError assertionError) {
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
     * @param isAlwaysNAKResponse
     *        if true, the Servers next response will always be a NAK.
     */
    public synchronized void setIsAlwaysNAKResponse(final boolean isAlwaysNAKResponse) {
        this.setIsAlwaysNAKResponse = isAlwaysNAKResponse;
    }

    /**
     * The server <b>never</b> return a response, when true.
     *
     * @return true when the server will <b>never</b> return a response. Default is false.
     */
    public boolean getIsAlwaysNoResponse() {
        return this.setIsAlwaysNoResponse;
    }

    /**
     * The server <b>never</b> return a response, when true.
     *
     * @param isAlwaysNoResponse
     *        true when the server will <b>never</b> return a response. Default is false.
     */
    public synchronized void setIsAlwaysNoResponse(final boolean isAlwaysNoResponse) {
        this.setIsAlwaysNoResponse = isAlwaysNoResponse;
    }

    /**
     * Forces the Server to close down after processing the next message received (regardless of <u>any</u> other conditions). The next message will first be processed as normal; irrespective of this property.
     * <p>
     * This is intended to be used so that test clients can wait on the server Thread to end.
     * <p>
     * Default is false.
     *
     * @return if true, the Server will close after the message processing is complete. Default is false.
     */
    public boolean getIsCloseAfterNextResponse() {
        return this.isCloseAfterNextResponse;
    }

    /**
     * Forces the Server to close down after processing the next message received (regardless of <u>any</u> other conditions). The next message will first be processed as normal; irrespective of this property.
     * <p>
     * This is intended to be used so that test clients can wait on the server Thread to end.
     * <p>
     * Default is false.
     *
     * @param isCloseAfterNextResponse
     *        if true, the Server will close after the message processing is complete. Default is false.
     */
    public synchronized void setIsCloseAfterNextResponse(final boolean isCloseAfterNextResponse) {
        this.isCloseAfterNextResponse = isCloseAfterNextResponse;
    }

    /**
     * If any message, other that this one, is the next message to be received, record it as an {@link MockTCPServer#setAssertionError(AssertionError) assertion error}.
     *
     * @return ignore if null.
     */
    public DataStreamRegexMatcher getExpectedMessage() {
        return this.expectedMessage;
    }

    /**
     * If any message, other that this one, is the next message to be received, record it as an {@link MockTCPServer#setAssertionError(AssertionError) assertion error}.
     *
     * @param expectedMessage
     *        a Regular Expression that describes what the next received message will be.
     */

    public synchronized void setExpectedMessage(final String expectedMessage) {
        this.expectedMessage = new DataStreamRegexMatcher(expectedMessage);
    }

    /**
     * If any message, other that this one, is the next message to be received, record it as an {@link MockTCPServer#setAssertionError(AssertionError) assertion error}.
     *
     * @param expectedMessage
     *        a Regular Expression that describes what the next received message will be.
     */
    public synchronized void setExpectedMessage(final StringBuffer expectedMessage) {
        setExpectedMessage(expectedMessage.toString());
    }

    /**
     * The number of messages received by the server since the server was started.
     *
     * @return The number of messages received by the server since the server was started. Default is 0.
     */
    public int getMessagesReceivedCount() {
        return this.messagesReceivedCount;
    }

    /**
     * Add one to the number of messages received by the server since the server was started.
     *
     * see {@link MockTCPServer#getMessagesReceivedCount()}
     */
    private void incrementMessagesReceivedCount() {
        this.messagesReceivedCount++;
    }

    /**
     * Close the socket (if it is open) and any open data streams.
     */
    @Override
    public synchronized void close() {
        this.logger.info("Closing...");

        setClosed(true);
        try {
            if ((getConnectionSocket() != null) && !getConnectionSocket().isInputShutdown()) {
                getConnectionSocket().shutdownInput();
            }
            if ((getConnectionSocket() != null) && !getConnectionSocket().isOutputShutdown()) {
                getConnectionSocket().shutdownOutput();
            }
        } catch (final Exception e) {
            handleException(e);
        }
        this.logger.info("Closing input stream...");
        IOUtils.closeQuietly(this.inputStream);
        this.logger.info("Closed input stream.");
        this.logger.info("Closing output stream...");
        IOUtils.closeQuietly(this.outputStream);
        this.logger.info("Closed output stream.");
        this.logger.info("Closing the socket...");
        IOUtils.closeQuietly(this.socket);
        this.logger.info("Closed the socket.");

        try {
            if (!super.isAlive()) {
                this.logger.trace(String.format("Server Thread \"%s\" is not alive.", super.getName()));
            }
            if (super.isInterrupted()) {
                this.logger.trace(String.format("Server Thread \"%s\" is not interrupted.", super.getName()));
            }
            super.interrupt();
            // Wait for the server thread to close.
            super.join();
        } catch (final InterruptedException e) {
            // Do nothing
        }

        this.logger.info("Closed.");
    }

    private boolean isClosed() {
        return this.isClosed;
    }

    private synchronized void setClosed(final boolean isClosed) {
        this.isClosed = isClosed;
    }

    private int getPort() {
        return this.port;
    }

    private void setPort(final int port) {
        this.port = port;
    }

    /**
     * Open the Server Socket and wait for a connection.
     * <p>
     * The socket is opened on the configured {@link #getPort() port} on localhost.
     *
     * @return a new ServerSocket.
     * @throws IOException
     */
    private ServerSocket getSocket() throws IOException {
        if (this.socket == null) {
            this.logger.info(String.format("Opening a socket on port %d...", getPort()));
            setSocket(new ServerSocket(getPort()));
            this.logger.info("Waiting for a connection...");
            setConnectionSocket(this.socket.accept());
            this.logger.info(String.format("Accepted a connection."));
            setInputStream(new BufferedReader(new InputStreamReader(getConnectionSocket().getInputStream())));
            setOutputStream(new DataOutputStream(getConnectionSocket().getOutputStream()));
            this.logger.info("Ready to receive input.");
        }

        return this.socket;
    }

    private Socket getConnectionSocket() {
        return this.connectionSocket;
    }

    private void setConnectionSocket(final Socket connectionSocket) {
        this.connectionSocket = connectionSocket;
    }

    private void setSocket(final ServerSocket socket) {
        this.socket = socket;
    }

    private DataStream getDataStream() {
        if (this.dataStream == null) {
            this.dataStream = new DataStream(getTerminator().length, getRootLoggerName());
        }

        return this.dataStream;
    }

    private void setDataStream(final DataStream dataStream) {
        this.logger.debug("Closing the DataStream...");
        IOUtils.closeQuietly(this.dataStream);
        this.logger.debug("Closed the DataStream.");

        this.dataStream = dataStream;
    }

    private BufferedReader getInputStream() {
        return this.inputStream;
    }

    private void setInputStream(final BufferedReader inputStream) {
        this.inputStream = inputStream;
    }

    private DataOutputStream getOutputStream() {
        return this.outputStream;
    }

    private void setOutputStream(final DataOutputStream outputStream) {
        this.logger.info("Closing the output stream...");
        IOUtils.closeQuietly(this.outputStream);
        this.logger.info("Closed the output stream.");

        this.outputStream = outputStream;
    }

    /**
     * The log4j root logger name that will contain the class name, even if instantiated as an anonymous class.
     *
     * @return a root logger name.
     */
    public String getRootLoggerName() {
        return getThreadName().replaceAll("-", ".");
    }

    /**
     * Derives a {@link Thread#getName() Thread name} that includes the class name, even if this object instantiated as an anonymous class.
     *
     * @return a value used as the log4j root logger and the Thread name.
     */
    private String getThreadName() {
        final String delimeter = ".";
        final String regEx = "\\.";

        String name = null;

        if (StringUtils.isNotBlank(this.getClass().getSimpleName())) {
            name = this.getClass().getSimpleName();
        } else {
            if (this.getClass().getName().contains(delimeter)) {
                final String nameSegments[] = this.getClass().getName().split(regEx);

                name = String.format("%s-%s", this.getClass().getSuperclass().getSimpleName(), nameSegments[nameSegments.length - 1]);
            } else {
                name = this.getClass().getName();
            }
        }

        return name;
    }
}